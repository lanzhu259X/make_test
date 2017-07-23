package com.lanzhu.plugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;

public class MakeTestAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Application application = ApplicationManager.getApplication();
        MakeTestComponent component = application.getComponent(MakeTestComponent.class);
        component.startMakeTestThread(component.getPsiMethodFromContext(e));
    }
}
