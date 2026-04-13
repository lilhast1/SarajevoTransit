# Database Setup – Notification Service

Pokrenuti MySQL kontejner prije pokretanja aplikacije:
```bash
docker run --name notification-mysql -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=notification_db -e MYSQL_USER=notif_user -e MYSQL_PASSWORD=your_password -p 3312:3306 -d mysql:8
```

Nakon pokretanja aplikacije, Hibernate automatski kreira tabele i seed podaci se upisuju.