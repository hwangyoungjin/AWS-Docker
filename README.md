# AWS-Docker
## Mysql Utf-8 설정
```
* docker 컨테이너로 mysql 실행 후 
* workbench로 해당 스키마 utf8 설정
* 이후 table 생성시 자동 utf8 설정됨
```

# [First-Test](https://github.com/hwangyoungjin/AWS-Docker/blob/main/First/README.md)
## AWS EC2, ningxproxy, SpringBoot, React, Docker, Docker-Compose
### 설계
  - <img src="https://user-images.githubusercontent.com/60174144/128597490-aae271da-b634-420c-a41d-9d6d6c2c5d2d.png" width="70%" height="70%">

```java
client (80) -> (80) nginxproxy (3000)-> (3000) react 
client (80/api) -> (80) nginxproxy (8080) -> (8080) springboot (dockercompose links) -> mysql
```

---
# [Second-greenery](https://github.com/hwangyoungjin/AWS-Docker/blob/main/Second/README.md)
## AWS EC2, ningxproxy, SpringBoot, React, Docker, Github-Actions, HTTPS
### 설계 (production, develop 분리)
  - <img src="https://user-images.githubusercontent.com/60174144/147813781-3e74963a-1f54-478c-a7f6-10e2ce10b07f.png" width="70%" height="70%">
