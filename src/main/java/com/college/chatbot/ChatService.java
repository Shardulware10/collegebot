package com.college.chatbot;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ChatService {

    record Intent(String name, List<String> keys, String reply) {}

    public record Result(String intent, String reply) {}

    private static final List<Intent> INTENTS = List.of(
        new Intent("greeting", List.of("hi", "hello", "hey", "namaskar", "namaste"),
            "Namaskar! 👋 Mi CollegeBot ahe. Timetable, exam, fees, library, placement, hostel kinva contact vishayi vichar."),
        new Intent("timetable", List.of("timetable", "time table", "lecture", "schedule", "vel", "lectures"),
            "📅 Lectures roj 9 AM te 4 PM astat. Full timetable notice section madhe milel."),
        new Intent("exam", List.of("exam", "exams", "test", "paper", "pariksha", "result", "dates"),
            "📝 Exams semester end la hotat. Exact dates notice board var lagtat."),
        new Intent("fees", List.of("fees", "fee", "payment", "paise", "scholarship"),
            "💰 Fees accounts office madhe bharaychi. Last date notice board var check kara."),
        new Intent("library", List.of("library", "book", "books", "pustak"),
            "📚 Library 9 AM te 5 PM open asate. Id card ghevun ya."),
        new Intent("placement", List.of("placement", "job", "jobs", "company", "internship"),
            "💼 Placement cell chya notice varun registration kara. Internship chi mahiti tithe milel."),
        new Intent("hostel", List.of("hostel", "mess", "room", "canteen"),
            "🏠 Hostel ani mess sathi hostel warden la bheta. Canteen 8 AM te 6 PM open asate."),
        new Intent("contact", List.of("contact", "phone", "email", "office", "number"),
            "📞 Email: office@college.edu | Phone: 020-12345678"),
        new Intent("bye", List.of("bye", "thanks", "thank", "dhanyavad"),
            "Dhanyavad! 😊 Punha bhetu.")
    );

    private static final String FALLBACK =
        "Sorry, mala he samjal nahi 😕 Timetable, exam, fees, library, placement, hostel kinva contact vishayi vichar.";

    private final ChatRepository repo;

    public ChatService(ChatRepository repo) {
        this.repo = repo;
    }

    public Result answer(String text) {
        String t = text.toLowerCase();
        String[] tokens = t.split("[^a-z0-9]+");
        Intent best = null;
        int bestScore = 0;
        for (Intent in : INTENTS) {
            int score = 0;
            for (String key : in.keys()) {
                if (key.contains(" ")) {
                    if (t.contains(key)) score += 2;
                } else {
                    for (String tok : tokens) {
                        if (tok.equals(key)) { score += 2; break; }
                        if (key.length() >= 5 && tok.length() >= 4 && distance(tok, key) <= 1) { score += 1; break; }
                    }
                }
            }
            if (score > bestScore) { bestScore = score; best = in; }
        }
        return best == null ? new Result("unknown", FALLBACK) : new Result(best.name(), best.reply());
    }

    @Transactional
    public Result chat(String sessionId, String message) {
        repo.save(new ChatMessage(sessionId, "user", message, null));
        Result r = answer(message);
        repo.save(new ChatMessage(sessionId, "bot", r.reply(), r.intent()));
        return r;
    }

    public List<ChatMessage> history(String sessionId) {
        return repo.findBySessionIdOrderByIdAsc(sessionId);
    }

    @Transactional
    public void clear(String sessionId) {
        repo.deleteBySessionId(sessionId);
    }

    public Map<String, Object> stats() {
        List<Map<String, Object>> top = new ArrayList<>();
        for (Object[] row : repo.intentCounts()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("intent", row[0]);
            m.put("count", row[1]);
            top.add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalMessages", repo.count());
        out.put("userQuestions", repo.countBySender("user"));
        out.put("sessions", repo.countSessions());
        out.put("intents", top);
        return out;
    }

    private static int distance(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev; prev = cur; cur = tmp;
        }
        return prev[b.length()];
    }
}
