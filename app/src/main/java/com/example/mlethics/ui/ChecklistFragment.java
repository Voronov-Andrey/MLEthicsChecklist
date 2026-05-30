package com.example.mlethics.ui;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.mlethics.MainActivity;
import com.example.mlethics.data.DatabaseHelper;
import com.example.mlethics.model.Answer;
import com.example.mlethics.model.ChecklistItem;

import java.util.ArrayList;
import java.util.List;

public class ChecklistFragment extends Fragment {
    private DatabaseHelper db;
    private long auditId;
    private Spinner categorySpinner;
    private Spinner filterSpinner;
    private ItemAdapter adapter;
    private final List<ChecklistItem> items = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        MainActivity activity = (MainActivity) getActivity();
        db = activity.getDb();
        auditId = activity.getSelectedAuditId();

        LinearLayout root = UiUtils.vertical(activity, 12);
        root.addView(UiUtils.title(activity, "Чек-лист этической проверки", 20));
        if (auditId <= 0) {
            root.addView(UiUtils.text(activity, "Сначала создайте или выберите проект.", 15, Color.rgb(82, 95, 107)));
            return root;
        }

        LinearLayout filters = UiUtils.horizontal(activity, 0);
        filters.setPadding(0, UiUtils.dp(activity, 10), 0, UiUtils.dp(activity, 8));
        categorySpinner = new Spinner(activity);
        filterSpinner = new Spinner(activity);
        filters.addView(categorySpinner, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        filters.addView(filterSpinner, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        root.addView(filters);

        ListView listView = new ListView(activity);
        adapter = new ItemAdapter();
        listView.setAdapter(adapter);
        root.addView(listView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        setupSpinners();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ((MainActivity) getActivity()).showChecklistItem(items.get(position).id);
            }
        });
        loadItems();
        return root;
    }

    private void setupSpinners() {
        List<String> categories = new ArrayList<>();
        categories.add("Все");
        categories.addAll(db.getCategories());
        categorySpinner.setAdapter(new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item, categories));
        filterSpinner.setAdapter(new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Все", "Незаполненные", "Проблемные"}));
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadItems();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        categorySpinner.setOnItemSelectedListener(listener);
        filterSpinner.setOnItemSelectedListener(listener);
    }

    private void loadItems() {
        if (categorySpinner == null || filterSpinner == null) {
            return;
        }
        items.clear();
        items.addAll(db.getChecklistItems(
                String.valueOf(categorySpinner.getSelectedItem()),
                String.valueOf(filterSpinner.getSelectedItem()),
                auditId));
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private class ItemAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return items.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ChecklistItem item = items.get(position);
            Answer answer = db.getAnswer(auditId, item.id);
            LinearLayout row = UiUtils.vertical(getActivity(), 12);
            row.setBackgroundColor(Color.WHITE);
            row.addView(UiUtils.title(getActivity(), item.title, 16));
            row.addView(UiUtils.text(getActivity(), item.category + " • Важность: " + item.importance,
                    13, Color.rgb(82, 95, 107)));
            String status = answer == null ? DatabaseHelper.ANSWER_EMPTY : answer.answer;
            int color = DatabaseHelper.ANSWER_NO.equals(status) ? Color.rgb(180, 45, 45)
                    : DatabaseHelper.ANSWER_PARTLY.equals(status) ? Color.rgb(170, 115, 20)
                    : DatabaseHelper.ANSWER_YES.equals(status) ? Color.rgb(34, 130, 95)
                    : Color.rgb(120, 130, 140);
            row.addView(UiUtils.text(getActivity(), "Ответ: " + status, 14, color));
            row.setPadding(UiUtils.dp(getActivity(), 12), UiUtils.dp(getActivity(), 10),
                    UiUtils.dp(getActivity(), 12), UiUtils.dp(getActivity(), 10));
            return row;
        }
    }
}
