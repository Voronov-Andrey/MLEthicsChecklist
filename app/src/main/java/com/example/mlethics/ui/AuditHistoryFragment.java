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
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mlethics.MainActivity;
import com.example.mlethics.data.DatabaseHelper;
import com.example.mlethics.model.Audit;

import java.util.ArrayList;
import java.util.List;

public class AuditHistoryFragment extends Fragment {
    private DatabaseHelper db;
    private long projectId;
    private AuditAdapter adapter;
    private final List<Audit> audits = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        MainActivity activity = (MainActivity) getActivity();
        db = activity.getDb();
        projectId = activity.getSelectedProjectId();

        LinearLayout root = UiUtils.vertical(activity, 12);
        root.addView(UiUtils.title(activity, "История аудитов", 20));
        if (projectId <= 0) {
            root.addView(UiUtils.text(activity, "Сначала выберите проект.", 15, Color.rgb(82, 95, 107)));
            return root;
        }

        Button addButton = new Button(activity);
        addButton.setText("Добавить повторный аудит");
        addButton.setAllCaps(false);
        root.addView(addButton);

        ListView listView = new ListView(activity);
        adapter = new AuditAdapter();
        listView.setAdapter(adapter);
        root.addView(listView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAuditDialog();
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ((MainActivity) getActivity()).selectAudit(audits.get(position).id);
                Toast.makeText(getActivity(), "Аудит выбран", Toast.LENGTH_SHORT).show();
                ((MainActivity) getActivity()).showChecklist();
            }
        });

        loadAudits();
        return root;
    }

    private void loadAudits() {
        audits.clear();
        audits.addAll(db.getAudits(projectId));
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void showAuditDialog() {
        LinearLayout form = UiUtils.vertical(getActivity(), 16);
        final EditText title = field("Название аудита", "Повторный аудит");
        final EditText version = field("Версия модели", "");
        final EditText notes = field("Заметки", "");
        form.addView(title);
        form.addView(version);
        form.addView(notes);

        new AlertDialog.Builder(getActivity())
                .setTitle("Новый аудит")
                .setView(form)
                .setPositiveButton("Создать", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        long id = db.createAudit(projectId, title.getText().toString(),
                                version.getText().toString(), notes.getText().toString());
                        ((MainActivity) getActivity()).selectAudit(id);
                        Toast.makeText(getActivity(), "Аудит создан", Toast.LENGTH_SHORT).show();
                        loadAudits();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private EditText field(String hint, String value) {
        EditText input = new EditText(getActivity());
        input.setHint(hint);
        input.setText(value);
        input.setSingleLine(false);
        return input;
    }

    private class AuditAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return audits.size();
        }

        @Override
        public Object getItem(int position) {
            return audits.get(position);
        }

        @Override
        public long getItemId(int position) {
            return audits.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Audit audit = audits.get(position);
            LinearLayout row = UiUtils.vertical(getActivity(), 12);
            row.setBackgroundColor(Color.WHITE);
            row.addView(UiUtils.title(getActivity(), audit.title, 16));
            String line = UiUtils.safe(audit.modelVersion) + " • " + audit.createdAt +
                    " • риск: " + db.getRiskLevel(audit.id);
            row.addView(UiUtils.text(getActivity(), line, 14, Color.rgb(34, 87, 122)));
            row.addView(UiUtils.text(getActivity(), UiUtils.safe(audit.notes), 14, Color.rgb(82, 95, 107)));
            if (audit.id == ((MainActivity) getActivity()).getSelectedAuditId()) {
                row.addView(UiUtils.text(getActivity(), "Активный аудит", 13, Color.rgb(34, 130, 95)));
            }
            row.setPadding(UiUtils.dp(getActivity(), 12), UiUtils.dp(getActivity(), 10),
                    UiUtils.dp(getActivity(), 12), UiUtils.dp(getActivity(), 10));
            return row;
        }
    }
}
