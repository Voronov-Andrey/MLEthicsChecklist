package com.example.mlethics.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.mlethics.model.Answer;
import com.example.mlethics.model.Audit;
import com.example.mlethics.model.CategoryStats;
import com.example.mlethics.model.ChecklistItem;
import com.example.mlethics.model.Issue;
import com.example.mlethics.model.Project;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "ml_ethics_checklist.db";
    public static final int DB_VERSION = 1;

    public static final String ANSWER_YES = "Да";
    public static final String ANSWER_NO = "Нет";
    public static final String ANSWER_PARTLY = "Частично";
    public static final String ANSWER_NA = "Не применимо";
    public static final String ANSWER_EMPTY = "Не заполнено";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE projects (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "description TEXT," +
                "task_type TEXT," +
                "domain TEXT," +
                "target_variable TEXT," +
                "sensitive_features TEXT," +
                "created_at TEXT NOT NULL)");

        db.execSQL("CREATE TABLE audits (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "project_id INTEGER NOT NULL," +
                "title TEXT NOT NULL," +
                "model_version TEXT," +
                "created_at TEXT NOT NULL," +
                "notes TEXT," +
                "FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE)");

        db.execSQL("CREATE TABLE checklist_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "category TEXT NOT NULL," +
                "title TEXT NOT NULL," +
                "description TEXT," +
                "recommendation TEXT," +
                "importance INTEGER NOT NULL DEFAULT 1," +
                "template_type TEXT NOT NULL DEFAULT 'general')");

        db.execSQL("CREATE TABLE audit_answers (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "audit_id INTEGER NOT NULL," +
                "checklist_item_id INTEGER NOT NULL," +
                "answer TEXT NOT NULL," +
                "comment TEXT," +
                "needs_action INTEGER NOT NULL DEFAULT 0," +
                "updated_at TEXT NOT NULL," +
                "UNIQUE(audit_id, checklist_item_id)," +
                "FOREIGN KEY(audit_id) REFERENCES audits(id) ON DELETE CASCADE," +
                "FOREIGN KEY(checklist_item_id) REFERENCES checklist_items(id) ON DELETE CASCADE)");

        db.execSQL("CREATE TABLE issues (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "audit_id INTEGER NOT NULL," +
                "checklist_item_id INTEGER NOT NULL," +
                "title TEXT NOT NULL," +
                "severity TEXT NOT NULL," +
                "status TEXT NOT NULL," +
                "recommendation TEXT," +
                "user_comment TEXT," +
                "UNIQUE(audit_id, checklist_item_id)," +
                "FOREIGN KEY(audit_id) REFERENCES audits(id) ON DELETE CASCADE," +
                "FOREIGN KEY(checklist_item_id) REFERENCES checklist_items(id) ON DELETE CASCADE)");

        seedChecklist(db);
        seedDemoProject(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS issues");
        db.execSQL("DROP TABLE IF EXISTS audit_answers");
        db.execSQL("DROP TABLE IF EXISTS checklist_items");
        db.execSQL("DROP TABLE IF EXISTS audits");
        db.execSQL("DROP TABLE IF EXISTS projects");
        onCreate(db);
    }

    public long createProject(String name, String description, String taskType, String domain,
                              String targetVariable, String sensitiveFeatures) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("description", description);
        values.put("task_type", taskType);
        values.put("domain", domain);
        values.put("target_variable", targetVariable);
        values.put("sensitive_features", sensitiveFeatures);
        values.put("created_at", now());
        long projectId = db.insert("projects", null, values);
        createAudit(projectId, "Первичный аудит", "v1.0", "Базовая проверка проекта перед внедрением");
        return projectId;
    }

    public void updateProject(Project project) {
        ContentValues values = new ContentValues();
        values.put("name", project.name);
        values.put("description", project.description);
        values.put("task_type", project.taskType);
        values.put("domain", project.domain);
        values.put("target_variable", project.targetVariable);
        values.put("sensitive_features", project.sensitiveFeatures);
        getWritableDatabase().update("projects", values, "id=?", args(project.id));
    }

    public void deleteProject(long projectId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("issues", "audit_id IN (SELECT id FROM audits WHERE project_id=?)", args(projectId));
        db.delete("audit_answers", "audit_id IN (SELECT id FROM audits WHERE project_id=?)", args(projectId));
        db.delete("audits", "project_id=?", args(projectId));
        db.delete("projects", "id=?", args(projectId));
    }

    public List<Project> getProjects(String query) {
        List<Project> projects = new ArrayList<>();
        String sql = "SELECT * FROM projects";
        String[] args = null;
        if (query != null && query.trim().length() > 0) {
            sql += " WHERE name LIKE ? OR domain LIKE ? ORDER BY created_at DESC";
            args = new String[]{"%" + query.trim() + "%", "%" + query.trim() + "%"};
        } else {
            sql += " ORDER BY created_at DESC";
        }
        Cursor c = getReadableDatabase().rawQuery(sql, args);
        try {
            while (c.moveToNext()) {
                projects.add(readProject(c));
            }
        } finally {
            c.close();
        }
        return projects;
    }

    public Project getProject(long projectId) {
        Cursor c = getReadableDatabase().query("projects", null, "id=?", args(projectId), null, null, null);
        try {
            return c.moveToFirst() ? readProject(c) : null;
        } finally {
            c.close();
        }
    }

    public long createAudit(long projectId, String title, String version, String notes) {
        ContentValues values = new ContentValues();
        values.put("project_id", projectId);
        values.put("title", title);
        values.put("model_version", version);
        values.put("created_at", now());
        values.put("notes", notes);
        return getWritableDatabase().insert("audits", null, values);
    }

    public Audit getLatestAudit(long projectId) {
        Cursor c = getReadableDatabase().query("audits", null, "project_id=?", args(projectId),
                null, null, "created_at DESC, id DESC", "1");
        try {
            return c.moveToFirst() ? readAudit(c) : null;
        } finally {
            c.close();
        }
    }

    public Audit getAudit(long auditId) {
        Cursor c = getReadableDatabase().query("audits", null, "id=?", args(auditId), null, null, null);
        try {
            return c.moveToFirst() ? readAudit(c) : null;
        } finally {
            c.close();
        }
    }

    public List<Audit> getAudits(long projectId) {
        List<Audit> audits = new ArrayList<>();
        Cursor c = getReadableDatabase().query("audits", null, "project_id=?", args(projectId),
                null, null, "created_at DESC, id DESC");
        try {
            while (c.moveToNext()) {
                audits.add(readAudit(c));
            }
        } finally {
            c.close();
        }
        return audits;
    }

    public List<ChecklistItem> getChecklistItems(String category, String filter, long auditId) {
        List<ChecklistItem> items = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT ci.* FROM checklist_items ci");
        List<String> params = new ArrayList<>();
        if ("Незаполненные".equals(filter)) {
            sql.append(" LEFT JOIN audit_answers aa ON aa.checklist_item_id=ci.id AND aa.audit_id=?");
            params.add(String.valueOf(auditId));
        } else if ("Проблемные".equals(filter)) {
            sql.append(" JOIN audit_answers aa ON aa.checklist_item_id=ci.id AND aa.audit_id=?");
            params.add(String.valueOf(auditId));
        }
        sql.append(" WHERE 1=1");
        if (category != null && !"Все".equals(category)) {
            sql.append(" AND ci.category=?");
            params.add(category);
        }
        if ("Незаполненные".equals(filter)) {
            sql.append(" AND aa.id IS NULL");
        } else if ("Проблемные".equals(filter)) {
            sql.append(" AND (aa.answer IN (?, ?) OR aa.needs_action=1)");
            params.add(ANSWER_NO);
            params.add(ANSWER_PARTLY);
        }
        sql.append(" ORDER BY ci.category, ci.importance DESC, ci.id");
        Cursor c = getReadableDatabase().rawQuery(sql.toString(), params.toArray(new String[0]));
        try {
            while (c.moveToNext()) {
                items.add(readChecklistItem(c));
            }
        } finally {
            c.close();
        }
        return items;
    }

    public ChecklistItem getChecklistItem(long itemId) {
        Cursor c = getReadableDatabase().query("checklist_items", null, "id=?", args(itemId), null, null, null);
        try {
            return c.moveToFirst() ? readChecklistItem(c) : null;
        } finally {
            c.close();
        }
    }

    public Set<String> getCategories() {
        Set<String> categories = new HashSet<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT DISTINCT category FROM checklist_items ORDER BY category", null);
        try {
            while (c.moveToNext()) {
                categories.add(c.getString(0));
            }
        } finally {
            c.close();
        }
        return categories;
    }

    public Answer getAnswer(long auditId, long itemId) {
        Cursor c = getReadableDatabase().query("audit_answers", null,
                "audit_id=? AND checklist_item_id=?", args(auditId, itemId), null, null, null);
        try {
            return c.moveToFirst() ? readAnswer(c) : null;
        } finally {
            c.close();
        }
    }

    public void saveAnswer(long auditId, ChecklistItem item, String answer, String comment, boolean needsAction) {
        ContentValues values = new ContentValues();
        values.put("audit_id", auditId);
        values.put("checklist_item_id", item.id);
        values.put("answer", answer);
        values.put("comment", comment);
        values.put("needs_action", needsAction ? 1 : 0);
        values.put("updated_at", now());
        getWritableDatabase().insertWithOnConflict("audit_answers", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        syncIssueForAnswer(auditId, item, answer, needsAction);
    }

    public List<Issue> getIssues(long auditId, String statusFilter) {
        List<Issue> issues = new ArrayList<>();
        String where = "i.audit_id=?";
        List<String> params = new ArrayList<>();
        params.add(String.valueOf(auditId));
        if (statusFilter != null && !"Все".equals(statusFilter)) {
            where += " AND i.status=?";
            params.add(statusFilter);
        }
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT i.*, ci.category FROM issues i " +
                        "JOIN checklist_items ci ON ci.id=i.checklist_item_id " +
                        "WHERE " + where + " ORDER BY " +
                        "CASE i.severity WHEN 'Критичная' THEN 1 WHEN 'Высокая' THEN 2 WHEN 'Средняя' THEN 3 ELSE 4 END, i.id DESC",
                params.toArray(new String[0]));
        try {
            while (c.moveToNext()) {
                issues.add(readIssue(c));
            }
        } finally {
            c.close();
        }
        return issues;
    }

    public void updateIssue(long issueId, String status, String severity, String comment) {
        ContentValues values = new ContentValues();
        values.put("status", status);
        values.put("severity", severity);
        values.put("user_comment", comment);
        getWritableDatabase().update("issues", values, "id=?", args(issueId));
    }

    public int getAnsweredCount(long auditId) {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM audit_answers WHERE audit_id=?", args(auditId));
        try {
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            c.close();
        }
    }

    public int getTotalChecklistCount() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM checklist_items", null);
        try {
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            c.close();
        }
    }

    public int getRiskScore(long auditId) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT SUM(CASE aa.answer WHEN ? THEN 2 * ci.importance WHEN ? THEN 1 * ci.importance ELSE 0 END) " +
                        "FROM audit_answers aa JOIN checklist_items ci ON ci.id=aa.checklist_item_id WHERE aa.audit_id=?",
                new String[]{ANSWER_NO, ANSWER_PARTLY, String.valueOf(auditId)});
        try {
            return c.moveToFirst() && !c.isNull(0) ? c.getInt(0) : 0;
        } finally {
            c.close();
        }
    }

    public int getMaxRiskScore() {
        Cursor c = getReadableDatabase().rawQuery("SELECT SUM(2 * importance) FROM checklist_items", null);
        try {
            return c.moveToFirst() && !c.isNull(0) ? c.getInt(0) : 0;
        } finally {
            c.close();
        }
    }

    public String getRiskLevel(long auditId) {
        int max = getMaxRiskScore();
        int score = getRiskScore(auditId);
        if (max == 0) {
            return "Низкий";
        }
        int percent = score * 100 / max;
        if (percent >= 45) {
            return "Высокий";
        }
        if (percent >= 20) {
            return "Средний";
        }
        return "Низкий";
    }

    public List<CategoryStats> getCategoryStats(long auditId) {
        List<CategoryStats> stats = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT ci.category, COUNT(ci.id), COUNT(aa.id), " +
                        "SUM(CASE aa.answer WHEN ? THEN 2 * ci.importance WHEN ? THEN 1 * ci.importance ELSE 0 END), " +
                        "SUM(2 * ci.importance) " +
                        "FROM checklist_items ci LEFT JOIN audit_answers aa ON aa.checklist_item_id=ci.id AND aa.audit_id=? " +
                        "GROUP BY ci.category ORDER BY ci.category",
                new String[]{ANSWER_NO, ANSWER_PARTLY, String.valueOf(auditId)});
        try {
            while (c.moveToNext()) {
                CategoryStats stat = new CategoryStats();
                stat.category = c.getString(0);
                stat.total = c.getInt(1);
                stat.answered = c.getInt(2);
                stat.riskScore = c.isNull(3) ? 0 : c.getInt(3);
                stat.maxRiskScore = c.isNull(4) ? 0 : c.getInt(4);
                stats.add(stat);
            }
        } finally {
            c.close();
        }
        return stats;
    }

    private void syncIssueForAnswer(long auditId, ChecklistItem item, String answer, boolean needsAction) {
        boolean isProblem = ANSWER_NO.equals(answer) || ANSWER_PARTLY.equals(answer) || needsAction;
        SQLiteDatabase db = getWritableDatabase();
        if (!isProblem) {
            db.delete("issues", "audit_id=? AND checklist_item_id=?", args(auditId, item.id));
            return;
        }
        ContentValues values = new ContentValues();
        values.put("audit_id", auditId);
        values.put("checklist_item_id", item.id);
        values.put("title", item.title);
        values.put("severity", item.importance >= 3 ? "Высокая" : item.importance == 2 ? "Средняя" : "Низкая");
        values.put("status", "Новая");
        values.put("recommendation", item.recommendation);
        db.insertWithOnConflict("issues", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private void seedChecklist(SQLiteDatabase db) {
        addItem(db, "Данные", "Описан источник обучающих данных",
                "Зафиксировано, откуда получены данные, кто отвечает за их качество и для каких условий они подходят.",
                "Добавьте источник, период сбора, владельца данных и ограничения применимости.", 2);
        addItem(db, "Данные", "Проверены пропуски и выбросы",
                "Пропуски, дубликаты и аномальные значения могут исказить выводы модели.",
                "Сформируйте правила обработки пропусков и проверьте влияние выбросов на целевую переменную.", 2);
        addItem(db, "Данные", "Проверен дисбаланс классов",
                "Если один класс представлен значительно меньше, модель может хуже распознавать редкие случаи.",
                "Используйте стратифицированную оценку, балансировку выборки или изменение функции потерь.", 3);
        addItem(db, "Данные", "Зафиксированы ограничения датасета",
                "Данные могут не покрывать все группы пользователей, регионы, временные периоды или сценарии.",
                "Опишите ограничения и сценарии, в которых модель нельзя применять без дополнительной проверки.", 2);

        addItem(db, "Предвзятость", "Определены потенциально чувствительные признаки",
                "Даже если признак не используется напрямую, его могут заменять косвенные признаки.",
                "Укажите чувствительные признаки и возможные прокси: возраст, регион, доход, образование, пол.", 3);
        addItem(db, "Предвзятость", "Проверено распределение данных по группам",
                "Слабопредставленные группы часто получают менее точные или нестабильные предсказания.",
                "Сравните размер групп, долю целевого класса и качество разметки по каждой группе.", 3);
        addItem(db, "Предвзятость", "Оценено качество модели для разных групп",
                "Высокая средняя точность не гарантирует одинаковую точность для отдельных групп.",
                "Сравните accuracy, precision, recall, FPR и FNR по группам.", 3);
        addItem(db, "Предвзятость", "Определены меры смягчения предвзятости",
                "Без плана смягчения найденные различия остаются только наблюдением.",
                "Рассмотрите reweighing, изменение порога решения, сбор дополнительных данных или пересмотр признаков.", 3);

        addItem(db, "Качество модели", "Описаны метрики качества",
                "Метрики должны соответствовать задаче и последствиям ошибок.",
                "Выберите метрики для общей оценки и отдельные метрики для критичных ошибок.", 2);
        addItem(db, "Качество модели", "Проверена устойчивость на новых данных",
                "Модель может хорошо работать на тесте, но деградировать после изменения данных.",
                "Используйте отложенную выборку, временную валидацию или проверку на данных другого периода.", 2);
        addItem(db, "Качество модели", "Описаны возможные последствия ошибок",
                "Ошибки модели имеют разную цену в зависимости от домена.",
                "Зафиксируйте, какие ошибки наиболее опасны и кто пострадает от неверного решения.", 3);

        addItem(db, "Интерпретируемость", "Доступно объяснение решения модели",
                "Пользователь или эксперт должен понимать, почему было принято значимое решение.",
                "Добавьте описание ключевых факторов, глобальную важность признаков или локальные объяснения.", 2);
        addItem(db, "Интерпретируемость", "Описаны ограничения интерпретации",
                "Объяснения могут быть приближенными и вводить в заблуждение при неправильном применении.",
                "Укажите, какие объяснения являются приблизительными и как их нельзя трактовать.", 1);

        addItem(db, "Приватность", "Минимизированы персональные данные",
                "Избыточные персональные данные повышают риск утечки и злоупотребления.",
                "Удалите признаки, которые не нужны для задачи, или замените их агрегированными значениями.", 3);
        addItem(db, "Приватность", "Определены сроки хранения данных",
                "Данные не должны храниться бессрочно без основания.",
                "Укажите срок хранения, ответственного и процедуру удаления.", 2);
        addItem(db, "Приватность", "Описаны правила доступа к данным",
                "Доступ к чувствительным данным должен быть ограничен по ролям.",
                "Определите роли, права доступа и журналирование действий.", 2);

        addItem(db, "Мониторинг", "Определены метрики мониторинга после запуска",
                "После внедрения распределение данных и качество модели могут измениться.",
                "Отслеживайте drift, качество по группам, долю ручных пересмотров и жалобы пользователей.", 2);
        addItem(db, "Мониторинг", "Есть план повторного аудита",
                "Этическая проверка должна повторяться после обновления модели или данных.",
                "Запланируйте повторный аудит для новых версий модели и важных изменений данных.", 2);
        addItem(db, "Мониторинг", "Назначен ответственный за проблемы",
                "Найденные риски должны иметь владельца и статус исправления.",
                "Назначьте ответственного, срок и критерий закрытия для каждой проблемы.", 2);
    }

    private void seedDemoProject(SQLiteDatabase db) {
        ContentValues project = new ContentValues();
        project.put("name", "Демо: оценка заявок");
        project.put("description", "Пример проекта для проверки этических рисков ML-системы.");
        project.put("task_type", "Классификация");
        project.put("domain", "Финансы");
        project.put("target_variable", "Одобрение заявки");
        project.put("sensitive_features", "Возраст, регион, доход");
        project.put("created_at", now());
        long projectId = db.insert("projects", null, project);

        ContentValues audit = new ContentValues();
        audit.put("project_id", projectId);
        audit.put("title", "Первичный аудит");
        audit.put("model_version", "v1.0");
        audit.put("created_at", now());
        audit.put("notes", "Стартовый аудит для демонстрации функций приложения.");
        db.insert("audits", null, audit);
    }

    private void addItem(SQLiteDatabase db, String category, String title, String description,
                         String recommendation, int importance) {
        ContentValues values = new ContentValues();
        values.put("category", category);
        values.put("title", title);
        values.put("description", description);
        values.put("recommendation", recommendation);
        values.put("importance", importance);
        values.put("template_type", "general");
        db.insert("checklist_items", null, values);
    }

    private Project readProject(Cursor c) {
        Project p = new Project();
        p.id = c.getLong(c.getColumnIndexOrThrow("id"));
        p.name = c.getString(c.getColumnIndexOrThrow("name"));
        p.description = c.getString(c.getColumnIndexOrThrow("description"));
        p.taskType = c.getString(c.getColumnIndexOrThrow("task_type"));
        p.domain = c.getString(c.getColumnIndexOrThrow("domain"));
        p.targetVariable = c.getString(c.getColumnIndexOrThrow("target_variable"));
        p.sensitiveFeatures = c.getString(c.getColumnIndexOrThrow("sensitive_features"));
        p.createdAt = c.getString(c.getColumnIndexOrThrow("created_at"));
        return p;
    }

    private Audit readAudit(Cursor c) {
        Audit a = new Audit();
        a.id = c.getLong(c.getColumnIndexOrThrow("id"));
        a.projectId = c.getLong(c.getColumnIndexOrThrow("project_id"));
        a.title = c.getString(c.getColumnIndexOrThrow("title"));
        a.modelVersion = c.getString(c.getColumnIndexOrThrow("model_version"));
        a.createdAt = c.getString(c.getColumnIndexOrThrow("created_at"));
        a.notes = c.getString(c.getColumnIndexOrThrow("notes"));
        return a;
    }

    private ChecklistItem readChecklistItem(Cursor c) {
        ChecklistItem item = new ChecklistItem();
        item.id = c.getLong(c.getColumnIndexOrThrow("id"));
        item.category = c.getString(c.getColumnIndexOrThrow("category"));
        item.title = c.getString(c.getColumnIndexOrThrow("title"));
        item.description = c.getString(c.getColumnIndexOrThrow("description"));
        item.recommendation = c.getString(c.getColumnIndexOrThrow("recommendation"));
        item.importance = c.getInt(c.getColumnIndexOrThrow("importance"));
        item.templateType = c.getString(c.getColumnIndexOrThrow("template_type"));
        return item;
    }

    private Answer readAnswer(Cursor c) {
        Answer answer = new Answer();
        answer.id = c.getLong(c.getColumnIndexOrThrow("id"));
        answer.auditId = c.getLong(c.getColumnIndexOrThrow("audit_id"));
        answer.checklistItemId = c.getLong(c.getColumnIndexOrThrow("checklist_item_id"));
        answer.answer = c.getString(c.getColumnIndexOrThrow("answer"));
        answer.comment = c.getString(c.getColumnIndexOrThrow("comment"));
        answer.needsAction = c.getInt(c.getColumnIndexOrThrow("needs_action")) == 1;
        answer.updatedAt = c.getString(c.getColumnIndexOrThrow("updated_at"));
        return answer;
    }

    private Issue readIssue(Cursor c) {
        Issue issue = new Issue();
        issue.id = c.getLong(c.getColumnIndexOrThrow("id"));
        issue.auditId = c.getLong(c.getColumnIndexOrThrow("audit_id"));
        issue.checklistItemId = c.getLong(c.getColumnIndexOrThrow("checklist_item_id"));
        issue.title = c.getString(c.getColumnIndexOrThrow("title"));
        issue.category = c.getString(c.getColumnIndexOrThrow("category"));
        issue.severity = c.getString(c.getColumnIndexOrThrow("severity"));
        issue.status = c.getString(c.getColumnIndexOrThrow("status"));
        issue.recommendation = c.getString(c.getColumnIndexOrThrow("recommendation"));
        issue.userComment = c.getString(c.getColumnIndexOrThrow("user_comment"));
        return issue;
    }

    private String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
    }

    private String[] args(long value) {
        return new String[]{String.valueOf(value)};
    }

    private String[] args(long first, long second) {
        return new String[]{String.valueOf(first), String.valueOf(second)};
    }
}
