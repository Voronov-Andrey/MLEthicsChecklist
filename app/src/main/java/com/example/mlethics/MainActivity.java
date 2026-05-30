package com.example.mlethics;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.mlethics.data.DatabaseHelper;
import com.example.mlethics.model.Audit;
import com.example.mlethics.model.Project;
import com.example.mlethics.ui.AuditHistoryFragment;
import com.example.mlethics.ui.ChecklistFragment;
import com.example.mlethics.ui.ChecklistItemFragment;
import com.example.mlethics.ui.IssuesFragment;
import com.example.mlethics.ui.ProjectDetailFragment;
import com.example.mlethics.ui.ProjectsFragment;
import com.example.mlethics.ui.ResultsFragment;
import com.example.mlethics.ui.UiUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {
    private static final String PREFS = "ml_ethics_prefs";
    private static final String KEY_PROJECT_ID = "project_id";
    private static final String KEY_AUDIT_ID = "audit_id";

    private DatabaseHelper db;
    private TextView subtitleText;
    private LinearLayout navigationBar;
    private final Map<String, Button> navButtons = new LinkedHashMap<>();
    private long selectedProjectId = -1;
    private long selectedAuditId = -1;
    private String currentScreen = "projects";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = new DatabaseHelper(this);
        subtitleText = findViewById(R.id.subtitleText);
        navigationBar = findViewById(R.id.navigationBar);
        restoreSelection();
        buildNavigation();
        if (savedInstanceState == null) {
            showProjects();
        }
    }

    public DatabaseHelper getDb() {
        return db;
    }

    public long getSelectedProjectId() {
        return selectedProjectId;
    }

    public long getSelectedAuditId() {
        return selectedAuditId;
    }

    public void selectProject(long projectId) {
        Project project = db.getProject(projectId);
        if (project == null) {
            return;
        }
        Audit audit = db.getLatestAudit(projectId);
        selectedProjectId = projectId;
        selectedAuditId = audit == null
                ? db.createAudit(projectId, "Первичный аудит", "v1.0", "")
                : audit.id;
        saveSelection();
        updateSubtitle();
    }

    public void selectAudit(long auditId) {
        Audit audit = db.getAudit(auditId);
        if (audit == null) {
            return;
        }
        selectedProjectId = audit.projectId;
        selectedAuditId = auditId;
        saveSelection();
        updateSubtitle();
    }

    public void showProjects() {
        showFragment(new ProjectsFragment(), "projects");
    }

    public void showChecklist() {
        showFragment(new ChecklistFragment(), "checklist");
    }

    public void showProjectDetails() {
        showFragment(new ProjectDetailFragment(), "projects");
    }

    public void showIssues() {
        showFragment(new IssuesFragment(), "issues");
    }

    public void showResults() {
        showFragment(new ResultsFragment(), "results");
    }

    public void showHistory() {
        showFragment(new AuditHistoryFragment(), "history");
    }

    public void showChecklistItem(long itemId) {
        showFragment(ChecklistItemFragment.newInstance(itemId), "checklist");
    }

    private void showFragment(Fragment fragment, String screen) {
        currentScreen = screen;
        highlightNavigation(screen);
        FragmentTransaction tx = getFragmentManager().beginTransaction();
        tx.replace(R.id.fragmentContainer, fragment);
        tx.commit();
    }

    private void buildNavigation() {
        addNavButton("projects", "Проекты", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProjects();
            }
        });
        addNavButton("checklist", "Чек-лист", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showChecklist();
            }
        });
        addNavButton("issues", "Проблемы", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showIssues();
            }
        });
        addNavButton("results", "Итоги", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showResults();
            }
        });
        addNavButton("history", "История", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHistory();
            }
        });
        highlightNavigation(currentScreen);
    }

    private void addNavButton(String key, String label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(UiUtils.dp(this, 12), UiUtils.dp(this, 6), UiUtils.dp(this, 12), UiUtils.dp(this, 6));
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, UiUtils.dp(this, 8), 0);
        navigationBar.addView(button, lp);
        navButtons.put(key, button);
    }

    private void highlightNavigation(String activeKey) {
        for (Map.Entry<String, Button> entry : navButtons.entrySet()) {
            boolean selected = entry.getKey().equals(activeKey);
            entry.getValue().setBackgroundResource(selected ? R.drawable.nav_selected : R.drawable.nav_default);
            entry.getValue().setTextColor(selected ? 0xFFFFFFFF : 0xFF22577A);
        }
    }

    private void restoreSelection() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        long savedProjectId = prefs.getLong(KEY_PROJECT_ID, -1);
        long savedAuditId = prefs.getLong(KEY_AUDIT_ID, -1);
        if (savedProjectId > 0 && db.getProject(savedProjectId) != null) {
            selectedProjectId = savedProjectId;
            Audit latest = db.getLatestAudit(savedProjectId);
            selectedAuditId = db.getAudit(savedAuditId) != null
                    ? savedAuditId
                    : latest == null ? db.createAudit(savedProjectId, "Первичный аудит", "v1.0", "") : latest.id;
        } else {
            List<Project> projects = db.getProjects(null);
            if (!projects.isEmpty()) {
                selectProject(projects.get(0).id);
                return;
            }
        }
        updateSubtitle();
    }

    private void saveSelection() {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putLong(KEY_PROJECT_ID, selectedProjectId)
                .putLong(KEY_AUDIT_ID, selectedAuditId)
                .apply();
    }

    private void updateSubtitle() {
        Project project = selectedProjectId > 0 ? db.getProject(selectedProjectId) : null;
        Audit audit = selectedAuditId > 0 ? db.getAudit(selectedAuditId) : null;
        if (project == null) {
            subtitleText.setText("Создайте проект, чтобы начать этическую проверку ML-системы");
            return;
        }
        String auditPart = audit == null ? "" : " • " + audit.title + " (" + UiUtils.safe(audit.modelVersion) + ")";
        subtitleText.setText(project.name + auditPart);
    }
}
