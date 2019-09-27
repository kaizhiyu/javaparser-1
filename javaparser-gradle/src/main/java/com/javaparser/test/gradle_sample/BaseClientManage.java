package com.javaparser.test.gradle_sample;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.utils.SourceRoot;

public class BaseClientManage {
	
	static Logger logger = LoggerFactory.getLogger(BaseClientManage.class);
	
	static File resourcefile = new File("");
	
	private String sourcePath = "resources";
	private String outputPath = "output";
	
	public void doBaseService(){
		SourceRoot sourceRoot = getSourceCode();
			
		// 3.2. 获取Impl中方法名及方法体的map
		Map<String,BlockStmt> allImplMethodBodyMap = getImplMethodBodyMap(sourceRoot);
		if(null == allImplMethodBodyMap || allImplMethodBodyMap.isEmpty()){
			logger.debug("{}文件没有方法");
			return;
		}
		// 3.3. 获取Impl中的impprt
		NodeList<ImportDeclaration> imports = getFileImports(sourceRoot);
		// 3.4. 获取impl中的fileld
		NodeList<BodyDeclaration> bdList = getImplFileds(sourceRoot);
		
		// 3.4. 根据map修改client
		changeBaseClientBodyStmt(imports,bdList,allImplMethodBodyMap,
				sourceRoot);
			
	}
	/**
	 * 获取impl文件的成员变量
	 * @param implFilePath
	 * @param sourceRoot
	 * @return
	 */
	private NodeList<BodyDeclaration> getImplFileds(SourceRoot sourceRoot){
		CompilationUnit cu = sourceRoot.parse(sourcePath,"YqtCorpAccountTSImpl.java");
		NodeList<BodyDeclaration> nbd = new NodeList<BodyDeclaration>();
        NodeList<TypeDeclaration<?>> types = cu.getTypes();
        for(TypeDeclaration type : types){
        	NodeList<AnnotationExpr> aas = type.getAnnotations();
        	for(int i = 0 ;i < aas.size();i++){
        		AnnotationExpr ae = aas.get(i);
        	}
        	NodeList<BodyDeclaration> members = type.getMembers();
        	for(BodyDeclaration body: members){
        		if(body instanceof FieldDeclaration){
        			nbd.add(body);
        		}
        	}
        }
		return nbd;
	}
	/**
	 * 获取impl文件的imports，并根据client需要进行修改
	 * @param implFilePath
	 * @param sourceRoot
	 * @return
	 */
	private NodeList<ImportDeclaration> getFileImports(SourceRoot sourceRoot){
		List<String> delImports = Arrays.asList("ThriftParam","ThriftReturn", "HeartThriftServerImpl","ThriftServer");
		CompilationUnit cu = sourceRoot.parse(sourcePath,"YqtCorpAccountTSImpl.java");
		NodeList<ImportDeclaration> ils = cu.getImports();
		for(int i = 0 ;i < ils.size();i++){
			ImportDeclaration imp = ils.get(i);
			String name = imp.getNameAsString();
			String[] names = name.split("\\.");
			String lastName = names[names.length-1];
			if(name.endsWith("TS") || delImports.contains(lastName)){
				ils.remove(i);
				i--;
			}
		}
		return ils;
	}
	/**
	 * 获取impl文件中各方法的body
	 * @param implFilePath
	 * @return
	 */
	private Map<String,BlockStmt> getImplMethodBodyMap(SourceRoot sourceRoot){
		Map<String,BlockStmt> implMethodBosyMap = new HashMap<String,BlockStmt>();
		CompilationUnit cu = sourceRoot.parse(sourcePath,"YqtCorpAccountTSImpl.java");
		// TODO:body若有getParamObj，则需要手动替换参数名称
		cu.accept(new ModifierVisitor<Void>() {
			@Override
			public Visitable visit(MethodDeclaration n, Void arg) {
				BlockStmt body = n.getBody().get();
				NodeList<Statement> ns = body.getStatements();
				for(int i =0; i < ns.size();i++){
        			Statement s = ns.get(i);
        			if(s instanceof TryStmt){
        				//去掉catch
        				BlockStmt inbs = ((TryStmt) s).getTryBlock();
        				// try语句中的返回去掉ThriftReturn.createSuccess
        				NodeList<Statement> inns = inbs.getStatements();
        				for(int j = 0;j < inns.size();j++){
        					Statement ins = inns.get(j);
        					if(ins instanceof ReturnStmt){
        						Expression exp = ((ReturnStmt) ins).getExpression().get();
        						NodeList<Expression> mas = ((MethodCallExpr)exp).getArguments();
        						if(mas.size() == 0){
        							// 无返回值
        							inns.remove(j);
        						}else if(mas.size() >= 1){
        							// 有返回值
        							((ReturnStmt) ins).setExpression(mas.get(0));
        						}
        					}
        				}
        				ns.set(i, inbs);
        			} else if(s instanceof IfStmt){
        				// 替换参数校验返回的错误信息
        				Statement is = ((IfStmt)s).getThenStmt();
        				BlockStmt inbs = is.toBlockStmt().get();
        				NodeList<Statement> inns = inbs.getStatements();
        				for(int j = 0;j < inns.size();j++){
        					Statement ins = inns.get(j);
        					if(ins instanceof ReturnStmt){
        						JavaParser jp = new JavaParser();
        						ParseResult<Statement> ps = jp.parseStatement("return JSONMessage.createFalied("
        								+ "PublicErrorCodeEnum.PARAM_EMPTY_ERROR.getCode(),"
        								+ "PublicErrorCodeEnum.PARAM_EMPTY_ERROR.getMsg()).toString();");
        						inns.set(j, ps.getResult().get());
        					}
        				}
        			} 
        		}
				implMethodBosyMap.put(n.getName().toString(),body);
				return super.visit(n, arg);
			}

		}, null);
		return implMethodBosyMap;
	}
	
	/**
	 * 修改client方法实现
	 * 1.修改body为对impl方法调用
	 * 2.去掉方法的throwExp
	 * 3.去掉extends
	 * @param imports					需要导入的类
	 * @param bdList					需要创建的成员变量
	 * @param allImplMethodBodyMap		各方法的body
	 * @param sourceRoot
	 */
	private void changeBaseClientBodyStmt(NodeList<ImportDeclaration> imports,
			NodeList<BodyDeclaration> bdList, Map<String,BlockStmt> allImplMethodBodyMap,SourceRoot sourceRoot){
		
		CompilationUnit cu = sourceRoot.parse(sourcePath,"YqtCorpAccountClient.java");
		
		cu.setImports(imports);
		cu.addImport("com.sinosun.utils.JSONMessage");
		cu.addImport("com.sinosun.contants.PublicErrorCodeEnum");
		// 向java文件添加成员变量
		NodeList<TypeDeclaration<?>> types = cu.getTypes();
		for(TypeDeclaration type : types){
			// 特殊处理，获取需要重命名方法名.修改类名
			String newName = "YqtCorpAccountService";
			type.setName(newName);
			
        	NodeList<BodyDeclaration> members = type.getMembers();
        	members.addAll(bdList);
        }
		
		cu.accept(new ModifierVisitor<Void>() {
			@Override
			public Visitable visit(MethodDeclaration n, Void arg) {
				// 获取方法名称
				String clientMethodName = n.getName().toString();
				
				// 去掉client文件中方法抛出的异常
				NodeList<ReferenceType> nullException = new NodeList<ReferenceType>();
				n.setThrownExceptions(nullException);
				// 修改body
				BlockStmt body = allImplMethodBodyMap.get(clientMethodName);
				if(null != body){
					n.setBody(body);
				}
				return super.visit(n, arg);
			}

			@Override
        	public Visitable visit(ClassOrInterfaceDeclaration n, Void arg){
				// 去掉extends
        		NodeList<ClassOrInterfaceType> ex = new NodeList<ClassOrInterfaceType>();
        		n.setExtendedTypes(ex);
        		// 去掉implication
        		n.setImplementedTypes(ex);
        		// 去掉@ThriftClient
        		Optional<AnnotationExpr> thriftAnno = n.getAnnotationByName("ThriftClient");
        		if(thriftAnno.isPresent()){
        			n.remove(thriftAnno.get());
        		}
        		return super.visit(n, arg);
        	}
		}, null);
		
		String output = resourcefile.getAbsolutePath() + "\\" + outputPath;
		URI uri = getURI(output);
		Path path = Paths.get(uri);
		logger.info(path.toString());
		sourceRoot.saveAll(Paths.get(uri));
	}
	
	private SourceRoot getSourceCode(){
		// 设置SourceRoot
		URI uri = getURI(resourcefile.getAbsolutePath());
		SourceRoot sourceRoot = new SourceRoot(Paths.get(uri));
		return sourceRoot;
	}
	
	public URI getURI(String filePath){
		try {
			filePath = URLEncoder.encode(filePath, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		String uriStr = "file:///" + filePath;
		URI uri = URI.create(uriStr);
		return uri;
	}
	/**
	 * 测试文件中，YqtCorpAccountClient继承自YqtCorpAccountTSImpl
	 * 程序运行目标为
	 * 1.用YqtCorpAccountTSImpl中的方法体替换YqtCorpAccountClient中的方法体
	 * 2.去掉YqtCorpAccountClient的继承声明
	 * 3.用YqtCorpAccountTSImpl中的import替换YqtCorpAccountClient中的import
	 * 4.替换方法体时，去掉YqtCorpAccountClient方法体中的try-catch
	 * 5.替换方法体时，修改返回值
	 * 6.修改YqtCorpAccountClient类名称为YqtCorpAccountService
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		
		BaseClientManage baseClientManage = new BaseClientManage();
		baseClientManage.doBaseService();
		System.out.println(FilenameUtils.getPathNoEndSeparator(resourcefile.getAbsolutePath()));
		
	}
}
