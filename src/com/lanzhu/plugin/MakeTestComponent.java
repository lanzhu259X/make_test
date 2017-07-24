package com.lanzhu.plugin;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MakeTestComponent implements ApplicationComponent {

    private static final String ANNOTATION_NAME = "@MakeTest";
    private static final String TEST_METHOD_PRE = "test";
    private static final String TEST_ANNOTATION = "Test";
    private static final String RETURN_VOID = "void";
    private static final String EXCEPTION_STR = "Exception";

    private static final String REQUEST_PARM_STR = "paramStr";
    private static final String REQUEST_INFO_STR = "requestInfo";

    private String basePath;

    @Override
    public void initComponent() {
        // do nothing
    }

    @Override
    public void disposeComponent() {
        // do nothing
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "com.lanzhu.plugin.MakTestComponent";
    }

    /**
     * 启动写的线程
     * @param psiClass
     */
    public void startMakeTestThread(final PsiClass psiClass) {
        new WriteCommandAction.Simple(psiClass.getProject(), psiClass.getContainingFile()) {
            @Override
            protected void run() throws Throwable {
                makeTestMethod(psiClass);
            }
        }.execute();
    }

    public void makeTestMethod(PsiClass psiClass) {
        PsiField field = getHaveMakeTestAnnotationFiel(psiClass);
        if (field == null) {
            return;
        }
        this.basePath = field.getProject().getBasePath();
        Set<String> existMethodSet = getMethodNameSet(psiClass);
        //获取 filed 属性的类的PsiClass
        PsiClass targetPisClass = PsiUtil.resolveClassInType(field.getType());
        List<PsiMethod> targetMethods = getPublicMethod(targetPisClass);
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiClass.getProject());
        for (PsiMethod targetMethod : targetMethods) {
            String testMethodName = TEST_METHOD_PRE + getFirstUpperCase(targetMethod.getName());
            if (existMethodSet.contains(testMethodName)) {
                continue;
            }
            String textMethod = buildTestMethod(targetMethod, field);
            PsiMethod toMethod = elementFactory.createMethodFromText(textMethod, psiClass);
            toMethod.getModifierList().addAnnotation(TEST_ANNOTATION);
            psiClass.add(toMethod);
        }
    }

    private String buildTestMethod(PsiMethod targetMethod, PsiField targetObj) {
        StringBuilder sb = new StringBuilder();
        String testMethodName = TEST_METHOD_PRE + getFirstUpperCase(targetMethod.getName());
        sb.append("public void ");
        sb.append(testMethodName);
        sb.append(" () {\n");
        sb.append("try {\n");
        sb.append(buildTryBodyStr(targetMethod, targetObj));
        sb.append("} ");
        sb.append(buildCatchBodyStr(targetMethod));
        sb.append(" catch (Exception e) {\n");
        sb.append("System.out.println(e.getMessage());\n");
        sb.append("}\n");
        sb.append("}\n");
        return sb.toString();

    }

    private String buildCatchBodyStr(PsiMethod targetMethod) {
        StringBuilder sb = new StringBuilder();
        PsiClassType[] psiClassTypes = targetMethod.getThrowsList().getReferencedTypes();
        if (psiClassTypes == null || psiClassTypes.length <= 0) {
            return "";
        }
        for (PsiClassType type : psiClassTypes) {
            String exceptionName = type.getPresentableText();
            if (EXCEPTION_STR.equalsIgnoreCase(exceptionName)) {
                continue;
            }
            sb.append(" catch (");
            sb.append(type.getCanonicalText());
            sb.append(" e) {\n");
            sb.append("System.out.println(e.getMessage());\n");
            sb.append("Assert.isTrue(true);\n");
            sb.append("} ");
        }
        return sb.toString();
    }

    private String buildTryBodyStr(PsiMethod targetMethod, PsiField targetObj) {
        StringBuilder sb = new StringBuilder();
        Map<String, String> map = buildRequestParamStr(targetMethod);
        if (targetMethod.getReturnType() == null) {
            // constructor method
            sb.append(map.get(REQUEST_INFO_STR));
            sb.append("new ");
            sb.append(targetMethod.getName());
            sb.append(" (");
            sb.append(map.get(REQUEST_PARM_STR));
            sb.append(");\n");
            return sb.toString();
        }else {
            sb.append(map.get(REQUEST_INFO_STR));
            String returnTypeName = targetMethod.getReturnType().getPresentableText();
            if (!returnTypeName.equalsIgnoreCase(RETURN_VOID)) {
                sb.append(returnTypeName);
                sb.append(" result = ");
            }
            if (targetMethod.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
                sb.append(targetObj.getType().getPresentableText());
            }else {
                sb.append(targetObj.getName());
            }
            sb.append(".");
            sb.append(targetMethod.getName());
            sb.append(" (");
            sb.append(map.get(REQUEST_PARM_STR));
            sb.append(");\n");
            sb.append("Assert.isTrue(true);\n");
        }

        return sb.toString();
    }

    private Map<String, String> buildRequestParamStr(PsiMethod targetMethod) {
        Map<String, String> result = new HashMap<>();
        if (targetMethod.getParameterList() == null || targetMethod.getParameterList().getParameters().length <= 0) {
            // request param is null.
            result.put(REQUEST_INFO_STR, "");
            result.put(REQUEST_PARM_STR, "");
            return result;
        }
        PsiParameter[]  parameters = targetMethod.getParameterList().getParameters();
        StringBuilder requestInfo = new StringBuilder();
        StringBuilder paramStr = new StringBuilder();
        for (int i = 0; i < parameters.length; i++) {
            PsiParameter parameter = parameters[i];
            requestInfo.append(buildRequestValue(parameter));
            paramStr.append(parameter.getName());
            if (i < (parameters.length - 1)) {
                paramStr.append(", ");
            }
        }
        result.put(REQUEST_INFO_STR, requestInfo.toString());
        result.put(REQUEST_PARM_STR, paramStr.toString());
        return result;
    }

    private String buildRequestValue(PsiParameter parameter) {
        StringBuilder sb = new StringBuilder();
        String paramTypeName = parameter.getType().getPresentableText();
        String paramName = parameter.getName();
        DefaultBaseType baseType = DefaultBaseType.getByCode(paramTypeName);
        if (baseType != null) {
            sb.append(paramTypeName);
            sb.append(" ");
            sb.append(paramName);
            sb.append(" = ");
            sb.append(baseType.getValue());
            sb.append(";\n");
            return sb.toString();
        }else {
            //not default base type
//            PsiClass paramPsiClass = PsiUtil.resolveClassInType(parameter.getType());
//            String fullName = paramPsiClass.getQualifiedName();
            sb.append(paramTypeName);
            sb.append(" ");
            sb.append(paramName);
            sb.append(" = null;\n");

//            if (fullName.startsWith(basePath)){
//                sb.append("new ");
//                sb.append(paramTypeName);
//                sb.append("();\n");
//                sb.append(buildParamObjectSet(paramPsiClass));
//            }else {
//                sb.append("null;\n");
//            }
            return sb.toString();
        }
    }

    private String buildParamObjectSet(PsiClass paramPsiClass) {
        StringBuilder sb = new StringBuilder();
        PsiField[] fields = paramPsiClass.getFields();
        //TODO
        return sb.toString();
    }


    private String getFirstUpperCase(String oldStr) {
        return oldStr.substring(0, 1).toUpperCase() + oldStr.substring(1);
    }

    private Set<String> getMethodNameSet(PsiClass psiClass) {
        if (psiClass == null) {
            return Collections.emptySet();
        }
        PsiMethod[] psiMethods = psiClass.getMethods();
        if (psiMethods == null || psiMethods.length <=0) {
            return Collections.emptySet();
        }
        Set<String> set = new HashSet<>();
        for (PsiMethod method : psiMethods) {
            set.add(method.getName());
        }
        return set;
    }

    /**
     * get PsiClass public method
     * @param psiClass
     * @return
     */
    private List<PsiMethod> getPublicMethod(PsiClass psiClass) {
        if (psiClass == null) {
            return Collections.emptyList();
        }
        PsiMethod[] psiMethods = psiClass.getMethods();
        if (psiMethods == null || psiMethods.length <= 0) {
            return Collections.emptyList();
        }
        List<PsiMethod> publicMethods = new ArrayList<>();
        for (PsiMethod psiMethod : psiMethods) {
            if (psiMethod.getModifierList().hasModifierProperty(PsiModifier.PUBLIC)) {
                publicMethods.add(psiMethod);
            }
        }
        return publicMethods;
    }

    /**
     * 获取到第一个存在有MakeTest标签的属性
     * @param psiClass
     * @return
     */
    private PsiField getHaveMakeTestAnnotationFiel(PsiClass psiClass) {
        PsiField[] psiFields = psiClass.getFields();
        if (psiFields == null || psiFields.length <= 0) {
            return null;
        }
        for (int i = 0; i < psiFields.length; i++) {
            PsiField field = psiFields[i];
            if (isFielHaveMakeTestAnnotation(field)) {
                return field;
            }
        }
        return null;
    }

    private boolean isFielHaveMakeTestAnnotation(PsiField psiField) {
        PsiAnnotation[] annotations = psiField.getModifierList().getAnnotations();
        if (annotations == null || annotations.length <=0 ) {
            return false;
        }
        for (int i = 0; i < annotations.length; i++) {
            PsiAnnotation annotation = annotations[i];
            if (ANNOTATION_NAME.equalsIgnoreCase(annotation.getText())) {
                return true;
            }
        }
        return false;
    }

    public PsiClass getPsiMethodFromContext(AnActionEvent e) {
        PsiElement elementAt = getPsiElement(e);
        if (elementAt == null) {
            return null;
        }
        return PsiTreeUtil.getParentOfType(elementAt, PsiClass.class);
    }

    private PsiElement getPsiElement(AnActionEvent e) {
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        if (psiFile == null || editor == null) {
            e.getPresentation().setEnabled(false);
            return null;
        }
        //获取当前光标处的PsiElement
        int offset = editor.getCaretModel().getOffset();
        return psiFile.findElementAt(offset);
    }
}
