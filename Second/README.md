## AWS EC2, nginxproxy, SpringBoot, React, Docker, Docker-compose, Github-Actions
---
## 설계
  
1. EC2 서버에 Docker, Docker-compose 설치 & docker login

2. MySQL와 nginx docekr-compose 설정
```docker
services:
  nginxproxy:
    depends_on:
      - db
    image: nginx:latest
    ports:
      - "80:80"
    restart: always
    volumes:
      - "./nginx/nginx.conf:/etc/nginx/nginx.conf"
  db:
    image: mysql:5.7
    volumes:
      - ./mysqldata:/var/lib/mysql
    environment:
      - MYSQL_DATABASE=greenerydb
      - MYSQL_ROOT_PASSWORD=green
    ports:
      - "3306:3306"
    container_name: dbcontainer
```

3. nginxproxy 설정 
  - docker-compose있는 디렉토리 nginx 폴더 안 nginx.conf
  ```nginx
  user nginx;
  worker_processes  auto;

  error_log  /var/log/nginx/error.log warn;
  pid        /var/run/nginx.pid;

  events { 
      worker_connections 1024; 
  }

  http {
      include       /etc/nginx/mime.types;
      default_type  application/octet-stream;
      log_format  main  '$remote_addr - $remote_user [$time_local] "$request" '
                        '$status $body_bytes_sent "$http_referer" "$request_uri" "$uri"'
                        '"$http_user_agent" "$http_x_forwarded_for"';
      access_log  /var/log/nginx/access.log  main;    
      sendfile on;
      keepalive_timeout 65;

      upstream docker-server {
          server server:8080;
      }

      upstream docker-client {
          server client:3000;
      }

      server {
          location /api/ { #client가 " :80/api/~ "으로 요청시 proxy가 server로  " /api/~ " 요청 전달
              #rewrite ^/blog(.*)$ $1 break; #server 내부적으로 요청시 /api가 붙지 않으므로 사용X
              proxy_pass         http://docker-server;
              proxy_redirect     off;
              proxy_set_header   Host $host;
              proxy_set_header   X-Real-IP $remote_addr;
              proxy_set_header   X-Forwarded-For $proxy_add_x_forwarded_for;
              proxy_set_header   X-Forwarded-Host $server_name;
          }
          
          location / { #client가 " :80/~ "으로 요청시 proxy가 front로  " /~ " 요청 전달
              proxy_pass         http://docker-client;
              proxy_redirect     off;
              proxy_set_header   Host $host;
              proxy_set_header   X-Real-IP $remote_addr;
              proxy_set_header   X-Forwarded-For $proxy_add_x_forwarded_for;
              proxy_set_header   X-Forwarded-Host $server_name;
          }
      }
  }
  ```        

4. server 프로젝트
```properties
# 환경
- springboot
- gradle
- jdk 8
- mysql

# properties
spring.jpa.hibernate.ddl-auto=create-drop

#mysql 설정
spring.datasource.url=jdbc:mysql://3.38.62.243:3306/greenerydb?useUnicode=true\
  &characterEncoding=utf8&useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=green

# gradle
	runtimeOnly 'mysql:mysql-connector-java'
```
  - dockerfile 설정



5. [github action - aws ec2 연결]
  - [Reference1](https://www.sunny-son.space/AWS/Github%20Action%20CICD/)
  - [Reference2](https://isntyet.github.io/deploy/github-action%EA%B3%BC-aws-code-deploy%EB%A5%BC-%EC%9D%B4%EC%9A%A9%ED%95%98%EC%97%AC-spring-boot-%EB%B0%B0%ED%8F%AC%ED%95%98%EA%B8%B0(2)/)
  - ### 1. EC2 IAM 설정
    - AWS root계정 에서 IAM 역할 추가
    - 유형 개체 : AWS 서비스
    - 사용 사례 : EC2
    - 정책 : "AWSCodeDeployFullAccess"
    - 이름 : "EC2-deploy"
  - ### 2. 인스턴스 IAM 역할 수정
    - 인스턴스 우측마우스 -> 보안 -> IAM 역할 수정 -> EC2-deploy 연결
  - ### 3. CodeDeploy IAM 설정
    - 1번과 같은 방식으로 
    - 사용 사례 : CodeDeploy 
    - 이름 : "code-deploy" 
  - ### 4. CodeDeploy 생성, 설정
    - "CodeDeploy" 으로 서비스 검색 -> 애플리케이션 -> 애플리케이션 생성
    - 이름: greenery
    - 플랫폼 : ec2/온프레미스
  - ### 5. 배포그룹 생성 
    - 이름: greenery-group
    - 서비스 역할(arn) : 대시보드 역할가서 code-deploy의 정책이름(AWSCodeDeployFullAccess)클릭해서 arn 복사해서 넣기
    - key : Name, value : greenery(인스턴스이름)
    - 로드밸런싱 비활성화
  - ### 8. AWS CLI용 IAM user 생성
    - IAM -> 사용자 추가 선택
    - 이름 : service-deploy-user
    - 유형 : 프로그래밍 방식 액세스

6. server 용 deploy code

7. front 용 deploy code

[docker-compose 실행후 db 접속시 에러 해결](https://stackoverflow.com/questions/59838692/mysql-root-password-is-set-but-getting-access-denied-for-user-rootlocalhost)
```
* 도커 & workbench에서 접속시 에러
access denied for user 'root'@'localhost'
```