package com.example.mlethics.ui;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mlethics.MainActivity;
import com.example.mlethics.data.DatabaseHelper;
import com.example.mlethics.model.Issue;

import java.util.ArrayList;
import java.util.List;

public class IssuesFragment extends Fragment {
    private DatabaseHelper db;
    private long auditId;
    private Spinner statusSpinner;
    private IssueAdapter adapter;
    private final List<Issue> issues = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        MainActivity activity = (MainActivity) getActivity();
        db = activity.getDb();
        auditId = activity.getSelectedAuditId();

        LinearLayout root = UiUtils.vertical(activity, 12);
        root.addView(UiUtils.title(activity, "Проблемы и рекомендации", 20));
        if (auditId <= 0) {
            root.addView(UiUtils.text(activity, "Сначала выберите проект.", 15, Color.rgb(82, 95, 107)));
            return root;
        }

        statusSpinner = new Spinner(activity);
        statusSpinner.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Все", "Новая", "В работе", "Исправлена", "Принято как ограничение"}));
        root.addView(statusSpinner);

        ListView listView = new ListView(activity);
        adapter = new IssueAdapter();
        listView.setAdapter(adapter);
        root.addView(listView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        statusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadIssues();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showIssueDialog(issues.get(position));
            }
        });

        loadIssues();
        return root;
    }

    private void loadIssues() {
        if (statusSpinner == null) {
            return;
        }
        issues.clear();
        issues.addAll(db.getIssues(auditId, String.valueOf(statusSpinner.getSelectedItem())));
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void showIssueDialog(final Issue issue) {
        LinearLayout form = UiUtils.vertical(getActivity(), 16);
        form.addView(UiUtils.title(getActivity(), issue.title, 17));
        form.addView(UiUtils.text(getActivity(), issue.recommendation, 14, Color.rgb(31, 41, 51)));

        final Spinner severity = new Spinner(getActivity());
        final String[] severities = {"Критичная", "Высокая", "Средняя", "Низкая"};
        severity.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, severities));
        severity.setSelection(indexOf(severities, issue.severity));
        form.addView(severity);

        final Spinner status = new Spinner(getActivity());
        final String[] statuses = {"Новая", "В работе", "Исправлена", "Принято как ограничение"};
        status.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, statuses));
        status.setSelection(indexOf(statuses, issue.status));
        form.addView(status);

        final EditText comment = new EditText(getActivity());
        comment.setHint("Комментарий к проблеме");
        comment.setMinLines(3);
        comment.setSingleLine(false);
        comment.setText(issue.userComment);
        form.addView(comment);

        new AlertDialog.Builder(getActivity())
                .setTitle("Редактировать проблему")
                .setView(form)
                .setPositiveButton("Сохранить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        db.updateIssue(issue.id, String.valueOf(status.getSelectedItem()),
                                String.valueOf(severity.getSelectedItem()), comment.getText().toString());
                        Toast.makeText(getActivity(), "Проблема обновлена", Toast.LENGTH_SHORT).show();
                        loadIssues();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private int indexOf(String[] values, String value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value)) {
                return i;
            }
        }
        return 0;
    }

    private class IssueAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return issues.size();
        }

        @Override
        public Object getItem(int position) {
            return issues.get(position);
        }

        @Override
        public long getItemId(int position) {
            return issues.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Issue issue = issues.get(position);
            LinearLayout row = UiUtils.vertical(getActivity(), 12);
            row.setBackgroundColor(Color.WHITE);
            row.addView(UiUtils.title(getActivity(), issue.title, 16));
            row.addView(UiUtils.text(getActivity(), issue.category + " • " + issue.severity + " • " + issue.status,
                    14, Color.rgb(34, 87, 122)));
            row.addView(UiUtils.text(getActivity(), issue.recommendation, 14, Color.rgb(82, 95, 107)));
            row.setPadding(UiUtils.dp(getActivity(), 12), UiUtils.dp(getActivity(), 10),
                    UiUtils.dp(getActivity(), 12), UiUtils.dp(getActivity(), 10));
            return row;
        }
    }
}
