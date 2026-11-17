# CollegeBot (Spring Boot + Docker + Cloud)

Local run (Java 17 + Maven lagto):
    mvn spring-boot:run
Browser: http://localhost:8080

Cloud deploy (Render, free):
1. Ha poora folder GitHub repo madhe upload kar
2. render.com -> New -> Web Service -> repo select
3. Runtime/Language: Docker, Plan: Free
4. Deploy -> public URL milel

API: POST /api/chat, GET/DELETE /api/history, GET /api/stats, GET /api/health
