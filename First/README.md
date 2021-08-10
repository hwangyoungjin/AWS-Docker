## AWS EC2, SpringBoot, React, Docker, Docker-Compose
---
## 설계
  - <img src="https://user-images.githubusercontent.com/60174144/128597490-aae271da-b634-420c-a41d-9d6d6c2c5d2d.png" width="70%" height="70%">

```java
client (80) -> (80) nginxproxy (3000)-> (3000) react 
client (80/api) -> (80) nginxproxy (8080) -> (8080) springboot (dockercompose links) -> mysql
```
## 1. AWS EC2 
- EC2의 Docker & Docker-compose 설치 & docker login
- AWS Inbound 80 port 열어주기 (test 용으로 3000,8080도 open)

## 2. AWS server에서 사용 할 DockerFile 생성
  - ### 1) Server 
    - #### 로컬에서 생성한 springboot jar 파일 넘기기 
      ```properties
      # 프로젝트 환경
      - springboot
      - jdk 11
      - gradle
      ```

      ```properties
      # application.properties 설정
      spring.jpa.hibernate.ddl-auto=create-drop
      spring.datasource.url=jdbc:mysql://ec2IP:3306/fundb?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC
      spring.datasource.username=root
      spring.datasource.password=PASSWORD
      spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

      #gradle 설정
      runtimeOnly 'mysql:mysql-connector-java'
      ```
      

    - #### springboot 프로젝트 안 최상위 디렉토리에 DockerFile 생성 
      ```dockerfile
      # springBoot dockerfile
      FROM openjdk:8-jdk-alpine
      #FROM openjdk:11-jdk as builder

      # jar 파일 변수 생성
      ARG JAR_FILE=build/libs/*.jar
      # jar_file 변수를 사용하여 해당 파일을 app.jar 이름으로 copy
      COPY ${JAR_FILE} app.jar
      # gradle 프로젝트 실행 명령
      ENTRYPOINT ["java","-jar","app.jar"]
      ```
  - ### 1) Front
    - #### react 프로젝트 폴더 최상위 디렉토리에 DockerFile 생성 
    ```properties
    # 프로젝트 환경
    - node npm
    - react
    ```

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
    ```docker
    # COPY시 해당 폴더는 제외
    node_modules
    ```

## 3. docker-compose 설정
```dockerfile
version: "3"

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
    restart: on-failuer
    build:
      context: ./cc-front
      dockerfile: Dockerfile
    ports:
      - "3000:3000" # if want direct access
    container_name: clientcontainer
    depends_on:
      - server
  
  server:
    restart: on-failuer
    build:
      context: ./cc-server
      dockerfile: Dockerfile
    links:
      - "db:mysqldb"
    ports:
      - "8080:8080" # if want direct access
    container_name: servercontainer
    depends_on:
      - db

  db:
    image: mysql:5.7
    restart: always
    volumes:
      - ./mysqldata:/var/lib/mysql
    environment:
      MYSQL_ROOT_PASSWORD=******
      MYSQL_DATABASE=fundb
    ports:
      - "3306:3306"
    container_name: dbcontainer
```
## 4. nginxproxy 설정 
  - ### docker-compose있는 디렉토리 nginx 폴더 안 nginx.conf
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

## 5. 디렉토리 구조
```bash
├── .
├── ..
├── docker-compose.yml
├── mysqldata
├── nginx
│   └── nginx.conf
├── cc-server (Springboot)
│   ├── .
│   ├── ..
│   ├── .dockerignore    
│   ├── .git
│   ├── .github
│   ├── .gitignore   
│   ├── .gradle
│   ├── .idea
│   ├── Dockerfile
│   ├── auth
│   ├── build
│   │   └── lib
│   │       └── ~.jar
│   ├── build.gradle
│   ├── desktop.ini
│   ├── gradle
│   ├── gradlew
│   ├── gradlew.bat
│   ├── out
│   ├── settings.gradle
│   └── src
│   
├── cc-front (React)
│   ├── .
│   ├── ..
│   ├── .dockerignore    
│   ├── .git
│   ├── .github
│   ├── .gitignore       
│   ├── Dockerfile       
│   ├── desktop.ini      
│   ├── node_modules     
│   ├── package-lock.json
│   ├── package.json     
│   ├── public
│   ├── src
│   └──  yarn.lock 
└── run.sh
``` 

### 참조
1. [EC2 느려지는 현상 해결](https://steemit.com/kr-dev/@segyepark/aws-ec2)