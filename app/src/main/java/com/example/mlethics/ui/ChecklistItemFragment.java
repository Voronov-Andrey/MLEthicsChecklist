package com.example.mlethics.ui;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mlethics.MainActivity;
import com.example.mlethics.data.DatabaseHelper;
import com.example.mlethics.model.Answer;
import com.example.mlethics.model.ChecklistItem;

public class ChecklistItemFragment extends Fragment {
    private static final String ARG_ITEM_ID = "item_id";

    public static ChecklistItemFragment newInstance(long itemId) {
        ChecklistItemFragment fragment = new ChecklistItemFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ITEM_ID, itemId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        final MainActivity activity = (MainActivity) getActivity();
        final DatabaseHelper db = activity.getDb();
        final long auditId = activity.getSelectedAuditId();
        long itemId = getArguments().getLong(ARG_ITEM_ID);
        final ChecklistItem item = db.getChecklistItem(itemId);
        final Answer current = db.getAnswer(auditId, itemId);

        ScrollView scroll = new ScrollView(activity);
        LinearLayout root = UiUtils.vertical(activity, 14);
        scroll.addView(root);

        if (item == null) {
            root.addView(UiUtils.text(activity, "Пункт чек-листа не найден.", 15, Color.rgb(82, 95, 107)));
            return scroll;
        }

        root.addView(UiUtils.title(activity, item.title, 20));
        root.addView(UiUtils.text(activity, item.category + " • Важность: " + item.importance,
                14, Color.rgb(34, 87, 122)));

        TextView descriptionTitle = UiUtils.title(activity, "Почему это важно", 16);
        descriptionTitle.setPadding(0, UiUtils.dp(activity, 14), 0, 0);
        root.addView(descriptionTitle);
        root.addView(UiUtils.text(activity, item.description, 15, Color.rgb(31, 41, 51)));

        TextView recTitle = UiUtils.title(activity, "Рекомендация", 16);
        recTitle.setPadding(0, UiUtils.dp(activity, 14), 0, 0);
        root.addView(recTitle);
        root.addView(UiUtils.text(activity, item.recommendation, 15, Color.rgb(31, 41, 51)));

        final RadioGroup answers = new RadioGroup(activity);
        answers.setOrientation(RadioGroup.VERTICAL);
        final RadioButton yes = radio(activity, DatabaseHelper.ANSWER_YES);
        final RadioButton no = radio(activity, DatabaseHelper.ANSWER_NO);
        final RadioButton partly = radio(activity, DatabaseHelper.ANSWER_PARTLY);
        final RadioButton na = radio(activity, DatabaseHelper.ANSWER_NA);
        answers.addView(yes);
        answers.addView(no);
        answers.addView(partly);
        answers.addView(na);
        answers.setPadding(0, UiUtils.dp(activity, 12), 0, 0);
        root.addView(answers);

        final CheckBox needsAction = new CheckBox(activity);
        needsAction.setText("Добавить в список проблем");
        needsAction.setTextSize(15);
        root.addView(needsAction);

        final EditText comment = new EditText(activity);
        comment.setHint("Комментарий аудитора");
        comment.setMinLines(3);
        comment.setSingleLine(false);
        root.addView(comment);

        if (current != null) {
            if (DatabaseHelper.ANSWER_YES.equals(current.answer)) {
                yes.setChecked(true);
            } else if (DatabaseHelper.ANSWER_NO.equals(current.answer)) {
                no.setChecked(true);
            } else if (DatabaseHelper.ANSWER_PARTLY.equals(current.answer)) {
                partly.setChecked(true);
            } else if (DatabaseHelper.ANSWER_NA.equals(current.answer)) {
                na.setChecked(true);
            }
            needsAction.setChecked(current.needsAction);
            comment.setText(current.comment);
        }

        LinearLayout buttons = UiUtils.horizontal(activity, 0);
        buttons.setPadding(0, UiUtils.dp(activity, 16), 0, UiUtils.dp(activity, 8));
        Button back = new Button(activity);
        back.setText("Назад");
        back.setAllCaps(false);
        Button save = new Button(activity);
        save.setText("Сохранить");
        save.setAllCaps(false);
        buttons.addView(back, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        buttons.addView(save, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        root.addView(buttons);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.showChecklist();
            }
        });
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int checkedId = answers.getCheckedRadioButtonId();
                if (checkedId <= 0) {
                    Toast.makeText(activity, "Выберите ответ", Toast.LENGTH_SHORT).show();
                    return;
                }
                RadioButton selected = answers.findViewById(checkedId);
                db.saveAnswer(auditId, item, selected.getText().toString(),
                        comment.getText().toString(), needsAction.isChecked());
                Toast.makeText(activity, "Ответ сохранён", Toast.LENGTH_SHORT).show();
                activity.showChecklist();
            }
        });

        return scroll;
    }

    private RadioButton radio(MainActivity activity, String text) {
        RadioButton button = new RadioButton(activity);
        button.setId(View.generateViewId());
        button.setText(text);
        button.setTextSize(16);
        return button;
    }
}
