package com.example.mlethics.ui;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.mlethics.MainActivity;
import com.example.mlethics.data.DatabaseHelper;
import com.example.mlethics.model.Audit;
import com.example.mlethics.model.Project;

public class ProjectDetailFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        final MainActivity activity = (MainActivity) getActivity();
        DatabaseHelper db = activity.getDb();
        Project project = db.getProject(activity.getSelectedProjectId());
        Audit audit = db.getAudit(activity.getSelectedAuditId());

        ScrollView scroll = new ScrollView(activity);
        LinearLayout root = UiUtils.vertical(activity, 14);
        scroll.addView(root);

        if (project == null) {
            root.addView(UiUtils.text(activity, "Проект не выбран.", 15, Color.rgb(82, 95, 107)));
            return scroll;
        }

        root.addView(UiUtils.title(activity, project.name, 22));
        root.addView(UiUtils.text(activity, UiUtils.safe(project.description), 15, Color.rgb(31, 41, 51)));

        addInfo(root, "Домен", project.domain);
        addInfo(root, "Тип задачи", project.taskType);
        addInfo(root, "Целевая переменная", project.targetVariable);
        addInfo(root, "Чувствительные признаки", project.sensitiveFeatures);
        addInfo(root, "Создан", project.createdAt);

        if (audit != null) {
            int total = db.getTotalChecklistCount();
            int answered = db.getAnsweredCount(audit.id);
            int progress = total == 0 ? 0 : answered * 100 / total;
            TextView auditTitle = UiUtils.title(activity, "Активный аудит", 17);
            auditTitle.setPadding(0, UiUtils.dp(activity, 16), 0, UiUtils.dp(activity, 4));
            root.addView(auditTitle);
            root.addView(UiUtils.text(activity,
                    audit.title + " • " + UiUtils.safe(audit.modelVersion) + " • риск: " + db.getRiskLevel(audit.id),
                    15, Color.rgb(34, 87, 122)));
            ProgressBar progressBar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
            progressBar.setMax(100);
            progressBar.setProgress(progress);
            root.addView(progressBar);
            root.addView(UiUtils.text(activity, "Заполнено пунктов: " + answered + "/" + total,
                    14, Color.rgb(82, 95, 107)));
        }

        LinearLayout buttons = UiUtils.horizontal(activity, 0);
        buttons.setPadding(0, UiUtils.dp(activity, 18), 0, 0);
        Button checklist = new Button(activity);
        checklist.setText("Чек-лист");
        checklist.setAllCaps(false);
        Button results = new Button(activity);
        results.setText("Итоги");
        results.setAllCaps(false);
        buttons.addView(checklist, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        buttons.addView(results, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        root.addView(buttons);

        checklist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.showChecklist();
            }
        });
        results.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.showResults();
            }
        });
        return scroll;
    }

    private void addInfo(LinearLayout root, String label, String value) {
        TextView text = UiUtils.text(root.getContext(), label + ": " + UiUtils.safe(value),
                15, Color.rgb(31, 41, 51));
        text.setPadding(0, UiUtils.dp(root.getContext(), 8), 0, 0);
        root.addView(text);
    }
}
