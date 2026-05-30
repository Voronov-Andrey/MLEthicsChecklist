package com.example.mlethics.ui;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
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
import com.example.mlethics.R;
import com.example.mlethics.data.DatabaseHelper;
import com.example.mlethics.model.Audit;
import com.example.mlethics.model.Project;

import java.util.ArrayList;
import java.util.List;

public class ProjectsFragment extends Fragment {
    private DatabaseHelper db;
    private ProjectAdapter adapter;
    private EditText searchInput;
    private final List<Project> projects = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        MainActivity activity = (MainActivity) getActivity();
        db = activity.getDb();

        LinearLayout root = UiUtils.vertical(activity, 12);

        TextView title = UiUtils.title(activity, "Проекты ML-аудита", 20);
        root.addView(title);

        TextView hint = UiUtils.text(activity,
                "Создайте проект, выберите активный аудит и проходите этический чек-лист по категориям.",
                14, Color.rgb(82, 95, 107));
        root.addView(hint);

        LinearLayout searchRow = UiUtils.horizontal(activity, 0);
        searchRow.setPadding(0, UiUtils.dp(activity, 12), 0, UiUtils.dp(activity, 8));
        searchInput = new EditText(activity);
        searchInput.setHint("Поиск по названию или домену");
        searchRow.addView(searchInput, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button searchButton = new Button(activity);
        searchButton.setText("Найти");
        searchButton.setAllCaps(false);
        searchRow.addView(searchButton);
        root.addView(searchRow);

        ListView listView = new ListView(activity);
        listView.setClipToPadding(false);
        listView.setPadding(0, 0, 0, UiUtils.dp(activity, 16));
        TextView addButton = createFab(activity);
        LinearLayout footer = new LinearLayout(activity);
        footer.setGravity(Gravity.RIGHT);
        footer.setPadding(0, UiUtils.dp(activity, 12), UiUtils.dp(activity, 8), UiUtils.dp(activity, 20));
        footer.addView(addButton, new LinearLayout.LayoutParams(
                UiUtils.dp(activity, 56),
                UiUtils.dp(activity, 56)));
        listView.addFooterView(footer, null, false);
        adapter = new ProjectAdapter();
        listView.setAdapter(adapter);
        root.addView(listView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadProjects();
            }
        });
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProjectDialog(null);
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Project project = projects.get(position);
                ((MainActivity) getActivity()).selectProject(project.id);
                Toast.makeText(getActivity(), "Проект выбран: " + project.name, Toast.LENGTH_SHORT).show();
                ((MainActivity) getActivity()).showProjectDetails();
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                showProjectActions(projects.get(position));
                return true;
            }
        });

        loadProjects();
        return root;
    }

    private TextView createFab(MainActivity activity) {
        TextView addButton = new TextView(activity);
        addButton.setText("+");
        addButton.setTextColor(Color.WHITE);
        addButton.setTextSize(32);
        addButton.setGravity(Gravity.CENTER);
        addButton.setBackgroundResource(R.drawable.fab_background);
        addButton.setElevation(UiUtils.dp(activity, 6));
        addButton.setContentDescription("Добавить проект");
        return addButton;
    }

    private void loadProjects() {
        projects.clear();
        String query = searchInput == null ? null : searchInput.getText().toString();
        projects.addAll(db.getProjects(query));
        adapter.notifyDataSetChanged();
    }

    private void showProjectActions(final Project project) {
        new AlertDialog.Builder(getActivity())
                .setTitle(project.name)
                .setItems(new String[]{"Редактировать", "Удалить"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            showProjectDialog(project);
                        } else {
                            confirmDelete(project);
                        }
                    }
                })
                .show();
    }

    private void confirmDelete(final Project project) {
        new AlertDialog.Builder(getActivity())
                .setTitle("Удалить проект?")
                .setMessage("Будут удалены связанные аудиты, ответы и проблемы.")
                .setPositiveButton("Удалить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        db.deleteProject(project.id);
                        Toast.makeText(getActivity(), "Проект удалён", Toast.LENGTH_SHORT).show();
                        loadProjects();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showProjectDialog(final Project project) {
        final boolean edit = project != null;
        LinearLayout form = UiUtils.vertical(getActivity(), 16);
        final EditText name = field("Название", edit ? project.name : "");
        final EditText domain = field("Домен", edit ? project.domain : "");
        final EditText task = field("Тип задачи", edit ? project.taskType : "");
        final EditText target = field("Целевая переменная", edit ? project.targetVariable : "");
        final EditText sensitive = field("Чувствительные признаки", edit ? project.sensitiveFeatures : "");
        final EditText description = field("Описание", edit ? project.description : "");
        form.addView(name);
        form.addView(domain);
        form.addView(task);
        form.addView(target);
        form.addView(sensitive);
        form.addView(description);

        new AlertDialog.Builder(getActivity())
                .setTitle(edit ? "Редактировать проект" : "Новый проект")
                .setView(form)
                .setPositiveButton("Сохранить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (name.getText().toString().trim().length() == 0) {
                            return;
                        }
                        if (edit) {
                            project.name = name.getText().toString();
                            project.domain = domain.getText().toString();
                            project.taskType = task.getText().toString();
                            project.targetVariable = target.getText().toString();
                            project.sensitiveFeatures = sensitive.getText().toString();
                            project.description = description.getText().toString();
                            db.updateProject(project);
                            ((MainActivity) getActivity()).selectProject(project.id);
                            Toast.makeText(getActivity(), "Проект обновлён", Toast.LENGTH_SHORT).show();
                        } else {
                            long id = db.createProject(name.getText().toString(), description.getText().toString(),
                                    task.getText().toString(), domain.getText().toString(),
                                    target.getText().toString(), sensitive.getText().toString());
                            ((MainActivity) getActivity()).selectProject(id);
                            Toast.makeText(getActivity(), "Проект создан", Toast.LENGTH_SHORT).show();
                        }
                        loadProjects();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private EditText field(String hint, String value) {
        EditText input = new EditText(getActivity());
        input.setHint(hint);
        input.setText(value == null ? "" : value);
        input.setSingleLine(false);
        return input;
    }

    private class ProjectAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return projects.size();
        }

        @Override
        public Object getItem(int position) {
            return projects.get(position);
        }

        @Override
        public long getItemId(int position) {
            return projects.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Project project = projects.get(position);
            LinearLayout row = UiUtils.vertical(getActivity(), 12);
            row.setBackgroundColor(Color.WHITE);
            TextView name = UiUtils.title(getActivity(), project.name, 17);
            row.addView(name);
            String info = UiUtils.safe(project.domain) + " • " + UiUtils.safe(project.taskType);
            row.addView(UiUtils.text(getActivity(), info, 14, Color.rgb(82, 95, 107)));
            Audit audit = db.getLatestAudit(project.id);
            if (audit != null) {
                int total = db.getTotalChecklistCount();
                int answered = db.getAnsweredCount(audit.id);
                String line = "Прогресс: " + answered + "/" + total + " • Риск: " + db.getRiskLevel(audit.id);
                row.addView(UiUtils.text(getActivity(), line, 14, Color.rgb(34, 87, 122)));
            }
            TextView foot = UiUtils.text(getActivity(), "Нажмите, чтобы открыть. Долгое нажатие: действия.", 12,
                    Color.rgb(120, 130, 140));
            row.addView(foot);
            row.setPadding(UiUtils.dp(getActivity(), 12), UiUtils.dp(getActivity(), 10),
                    UiUtils.dp(getActivity(), 12), UiUtils.dp(getActivity(), 10));
            return row;
        }
    }
}
