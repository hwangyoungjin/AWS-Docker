# AWS-Docker
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
## AWS EC2, ningxproxy, SpringBoot, React, Docker, Github-Actions
### 설계
  - <img src="https://user-images.githubusercontent.com/60174144/128975823-ffa741bd-7202-4472-943e-0e9466856dc4.png" width="70%" height="70%">
```html
# github-actions
1. push / pull-Request to main branch -> event trigger
2  aws ec2 ssh connect
3. build in ec2 server
4. docker-compose up --build -d 
```
