## AWS EC2, nginxproxy, SpringBoot, React, Docker, Docker-compose, Github-Actions
---
## 설계
  
## 1. EC2 서버에 Docker, Docker-compose 설치 & docker login
```java
  - client (80) -> (80) nginxproxy (3000)-> (3000) react 
  - client (80/api) -> (80) nginxproxy (8080) -> (8080) springboot (dockercompose links) -> mysql
```
## 2. MySQL와 nginx, client, server docekr-compose 설정
```dockerfile
services:
  nginxproxy:
    depends_on:
      - db
      - server
      - client
    image: nginx:latest
    ports:
      - "80:80"
    restart: always
    volumes:
      - "./nginx/nginx.conf:/etc/nginx/nginx.conf"
  client:
    restart: always
    build:
      context: /home/ubuntu/greenery-front
      dockerfile: Dockerfile
    ports:
      - "3000:3000" # if want direct access
    container_name: clientcontainer
    depends_on:
      - server

  server:
    restart: restart
    build:
      context: /home/ubuntu/greenery-server
      dockerfile: Dockerfile
    links:
      - "db:greenerydb"
    ports:
      - "8080:8080" # if want direct access
    container_name: servercontainer
    depends_on:
      - db
  db:
    image: mysql:5.7
    volumes:
      - ./greenerydb:/var/lib/mysql
    environment:
      - MYSQL_DATABASE=greenerydb
      - MYSQL_ROOT_PASSWORD=******
    ports:
      - "3306:3306"
    container_name: dbcontainer
```

## 3. nginxproxy 설정 
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
              #rewrite ^/api(.*)$ $1 break; #server 내부적으로 요청시 /api가 붙지 않으므로 사용X
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

## 4. server 프로젝트
```properties
# 환경
- springboot
- gradle
- jdk 8
- mysql

# properties
spring.jpa.hibernate.ddl-auto=create-drop
spring.datasource.url=jdbc:mysql://3.38.62.243:3306/greenerydb?useUnicode=true\
  &characterEncoding=utf8&useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=green

# gradle
	runtimeOnly 'mysql:mysql-connector-java'
```
  - dockerfile 설정

- ## 5. client 프로젝트

- #### dockerfile 설정

```dockerfile
  # pull official base image
  FROM node:13.12.0-alpine

  # set working directory
  WORKDIR /app

  # add `/app/node_modules/.bin` to $PATH
  ENV PATH /app/node_modules/.bin:$PATH

  # install app dependencies
  COPY package.json ./
  COPY package-lock.json ./
  RUN npm install --silent
  RUN npm install react-scripts@3.4.1 -g --silent

  # add app
  # ./dockerignore에 있는것은 제외하고 복사
  COPY . ./

  # start app
  CMD ["npm", "start"]
```

- #### .dockerignore
```dockerfile
  # COPY시 해당 폴더는 제외
  node_modules
```

## 6. [github actions로 ec2 접속]
  - 0. ec2 서버에 jdk 8 설치
  - 1. ec2 인스턴스 SSH key생성(pem)
  - 2. github repo secret key 설정
    - 1) host : server ip
    - 2) user : ubuntu
    - 3) ssh_key : .pem 파일 전체 내용(RSA 암호화된 코드)
  - 3. deploy 스크립트

```deploy
  name: deploy

  on:
    push:
      branches:
        - main


  jobs:
    SSH:
      runs-on: ubuntu-latest

      steps:
        - uses: actions/checkout@v2
        - name: Run scripts in server
          uses: appleboy/ssh-action@master
          with:
            key: ${{ secrets.SSH_KEY }}
            host: ${{ secrets.HOST }}
            username: ${{ secrets.USER }}
            script: |
              cd greenery-server
              git reset --hard
              git fetch
              git pull
              chmod +x gradlew
              sudo ./gradlew clean bootjar
              cd ..
              docker-compose up --build -d
```
## [추후 S3 연동해서 CI/CD]()   

## Reference
[docker-compose 실행후 db 접속시 에러 해결](https://stackoverflow.com/questions/59838692/mysql-root-password-is-set-but-getting-access-denied-for-user-rootlocalhost)
[ec2 업그레이드 - aws 유형 변경](https://nerd-mix.tistory.com/32)
```
* 도커 & workbench에서 접속시 에러
access denied for user 'root'@'localhost'
```