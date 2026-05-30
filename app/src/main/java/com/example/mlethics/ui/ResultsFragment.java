package com.example.mlethics.ui;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Intent;
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
import android.widget.Toast;

import com.example.mlethics.MainActivity;
import com.example.mlethics.data.DatabaseHelper;
import com.example.mlethics.model.Audit;
import com.example.mlethics.model.CategoryStats;
import com.example.mlethics.model.Issue;
import com.example.mlethics.model.Project;

import java.util.List;

public class ResultsFragment extends Fragment {
    private DatabaseHelper db;
    private long auditId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        MainActivity activity = (MainActivity) getActivity();
        db = activity.getDb();
        auditId = activity.getSelectedAuditId();

        ScrollView scroll = new ScrollView(activity);
        LinearLayout root = UiUtils.vertical(activity, 12);
        scroll.addView(root);
        root.addView(UiUtils.title(activity, "Итоговая оценка", 20));

        if (auditId <= 0) {
            root.addView(UiUtils.text(activity, "Сначала выберите проект.", 15, Color.rgb(82, 95, 107)));
            return scroll;
        }

        int total = db.getTotalChecklistCount();
        int answered = db.getAnsweredCount(auditId);
        int progress = total == 0 ? 0 : answered * 100 / total;
        int riskScore = db.getRiskScore(auditId);
        int maxRisk = db.getMaxRiskScore();
        int riskPercent = maxRisk == 0 ? 0 : riskScore * 100 / maxRisk;
        String level = db.getRiskLevel(auditId);

        root.addView(metric(activity, "Прогресс проверки", progress + "% (" + answered + "/" + total + ")"));
        root.addView(bar(activity, progress, Color.rgb(56, 163, 165)));
        root.addView(metric(activity, "Уровень риска", level + " (" + riskScore + "/" + maxRisk + ")"));
        root.addView(bar(activity, riskPercent, riskColor(level)));

        TextView byCategory = UiUtils.title(activity, "Риск по категориям", 17);
        byCategory.setPadding(0, UiUtils.dp(activity, 16), 0, UiUtils.dp(activity, 8));
        root.addView(byCategory);
        List<CategoryStats> stats = db.getCategoryStats(auditId);
        for (CategoryStats stat : stats) {
            int percent = stat.maxRiskScore == 0 ? 0 : stat.riskScore * 100 / stat.maxRiskScore;
            root.addView(metric(activity, stat.category,
                    stat.riskScore + "/" + stat.maxRiskScore + " • заполнено " + stat.answered + "/" + stat.total));
            root.addView(bar(activity, percent, Color.rgb(34, 87, 122)));
        }

        Button report = new Button(activity);
        report.setText("Сформировать текст отчёта");
        report.setAllCaps(false);
        root.addView(report);
        report.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showReport();
            }
        });

        Button email = new Button(activity);
        email.setText("Отправить отчёт по E-mail");
        email.setAllCaps(false);
        root.addView(email);
        email.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendReportByEmail();
            }
        });

        return scroll;
    }

    private TextView metric(MainActivity activity, String label, String value) {
        TextView view = UiUtils.text(activity, label + ": " + value, 15, Color.rgb(31, 41, 51));
        view.setPadding(0, UiUtils.dp(activity, 8), 0, UiUtils.dp(activity, 2));
        return view;
    }

    private ProgressBar bar(MainActivity activity, int value, int color) {
        ProgressBar progressBar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(value);
        progressBar.getProgressDrawable().setTint(color);
        return progressBar;
    }

    private int riskColor(String level) {
        if ("Высокий".equals(level)) {
            return Color.rgb(190, 55, 55);
        }
        if ("Средний".equals(level)) {
            return Color.rgb(200, 145, 35);
        }
        return Color.rgb(34, 130, 95);
    }

    private void showReport() {
        Toast.makeText(getActivity(), "Отчёт сформирован", Toast.LENGTH_SHORT).show();
        new AlertDialog.Builder(getActivity())
                .setTitle("Текст отчёта")
                .setMessage(buildReport())
                .setPositiveButton("OK", null)
                .show();
    }

    private void sendReportByEmail() {
        MainActivity activity = (MainActivity) getActivity();
        String report = buildReport();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_SUBJECT, buildSubject());
        intent.putExtra(Intent.EXTRA_TEXT, report);

        try {
            Toast.makeText(activity, "Выберите способ отправки", Toast.LENGTH_SHORT).show();
            startActivity(Intent.createChooser(intent, "Отправить отчёт"));
        } catch (ActivityNotFoundException e) {
            Intent fallback = new Intent(Intent.ACTION_SEND);
            fallback.setType("text/plain");
            fallback.putExtra(Intent.EXTRA_SUBJECT, buildSubject());
            fallback.putExtra(Intent.EXTRA_TEXT, report);
            try {
                startActivity(Intent.createChooser(fallback, "Отправить отчёт"));
            } catch (ActivityNotFoundException ignored) {
                Toast.makeText(activity, "Нет приложения для отправки отчёта", Toast.LENGTH_LONG).show();
            }
        }
    }

    private String buildSubject() {
        MainActivity activity = (MainActivity) getActivity();
        Project project = db.getProject(activity.getSelectedProjectId());
        String name = project == null ? "ML-проект" : project.name;
        return "Отчёт ML Ethics Checklist: " + name;
    }

    private String buildReport() {
        MainActivity activity = (MainActivity) getActivity();
        Project project = db.getProject(activity.getSelectedProjectId());
        Audit audit = db.getAudit(auditId);
        StringBuilder report = new StringBuilder();
        report.append("Отчёт ML Ethics Checklist\n\n");
        if (project != null) {
            report.append("Проект: ").append(project.name).append("\n");
            report.append("Домен: ").append(UiUtils.safe(project.domain)).append("\n");
            report.append("Тип задачи: ").append(UiUtils.safe(project.taskType)).append("\n");
            report.append("Целевая переменная: ").append(UiUtils.safe(project.targetVariable)).append("\n");
            report.append("Чувствительные признаки: ").append(UiUtils.safe(project.sensitiveFeatures)).append("\n\n");
        }
        if (audit != null) {
            report.append("Аудит: ").append(audit.title).append(" / ").append(UiUtils.safe(audit.modelVersion)).append("\n");
            report.append("Дата: ").append(audit.createdAt).append("\n\n");
        }
        report.append("Прогресс: ").append(db.getAnsweredCount(auditId)).append("/")
                .append(db.getTotalChecklistCount()).append("\n");
        report.append("Итоговый риск: ").append(db.getRiskLevel(auditId)).append("\n\n");
        report.append("Проблемы:\n");
        List<Issue> issues = db.getIssues(auditId, "Все");
        if (issues.isEmpty()) {
            report.append("Проблем не зафиксировано.\n");
        } else {
            for (Issue issue : issues) {
                report.append("- ").append(issue.title).append(" [").append(issue.severity)
                        .append(", ").append(issue.status).append("]\n");
                report.append("  Рекомендация: ").append(issue.recommendation).append("\n");
                if (issue.userComment != null && issue.userComment.trim().length() > 0) {
                    report.append("  Комментарий: ").append(issue.userComment).append("\n");
                }
            }
        }
        return report.toString();
    }
}
