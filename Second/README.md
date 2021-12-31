## Skill
  - #### AWS EC2
  - #### nginxproxy
  - #### SpringBoot
  - #### React
  - #### Docker
  - #### Docker-compose
  - #### Github-Actions
  - #### HTTPS
---
## 설계
  
## 1. EC2 서버에 Docker, Docker-compose 설치 & docker login
```java
  - client (80) -> (80) nginxproxy (3000)-> (3000) react 
  - client (80/api) -> (80) nginxproxy (8080) -> (8080) springboot (dockercompose links) -> mysql
```
## 2. MySQL와 nginx, client, server docekr-compose 설정
```yml
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
      - "3000:3000" # If you don't want to go through nginxproxy and want to approach right away,
    container_name: clientcontainer
    depends_on:
      - server

  server:
    restart: restart
    build:
      context: /home/ubuntu/greenery-server/demo
      dockerfile: Dockerfile
    links:
      - "db:greenerydb"
    ports:
      - "8080:8080" # If you don't want to go through nginxproxy and want to approach right away,
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
  ```conf
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
spring.datasource.password=****

# gradle
	runtimeOnly 'mysql:mysql-connector-java'
```
  - dockerfile 설정
```dockerfile
FROM openjdk:8-jdk-alpine
# application 이름으로 work directory 만들기
WORKDIR application
# jar 파일 변수로 지정
ARG JAR_FILE=build/libs/*.jar
# jar_file을 application.jar 이름으로 copy 하기
COPY ${JAR_FILE} application.jar
ENTRYPOINT ["java","-jar","application.jar"]
```

## 5. client 프로젝트

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
  - 2. ec2 서버에서 git 설치 후 server, clien 디렉토리 만들고
    - github repo clone
    - [private repo clone 방법](https://uhou.tistory.com/99)
      - [rsa key pair 기존 키 삭제 후 새로 생성 가능](https://yunzema.tistory.com/373)
  - 3. github repo secret key 설정
    - 1) host : server ip
    - 2) user : ubuntu
    - 3) ssh_key : .pem 파일 전체 내용(RSA 암호화된 코드)
  - 4. server deploy 스크립트

```yml
  name: deploy.yml

  on:
    push: 
    # master 브랜치에 push(merger) 되면 event trigger
      branches:
        - master

  # job은 ssh 1개
  jobs:
    SSH:
      runs-on: ubuntu-latest

      steps:
        # https://github.com/actions/checkout 사용
        - uses: actions/checkout@v2

        # https://github.com/appleboy/ssh-action 사용
        - name: Run scripts in server
          uses: appleboy/ssh-action@master
          with:
            key: ${{ secrets.SSH_KEY }}
            host: ${{ secrets.HOST }}
            username: ${{ secrets.USER }}
            script: | # EC2 에서 실행되는 script
              cd greenery-server
              git reset --hard
              git fetch
              git pull
              cd demo
              chmod +x gradlew
              sudo ./gradlew clean bootjar
              cd ~/greenery-db-nginx
              docker-compose up --build -d
```
  - 5. client deploy.yml
```yml
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
            cd greenery-client
            git add .
            git reset --hard
            git fetch
            git pull
            cd ~/greenery-db-nginx
            docker-compose up --build -d
```
## 7. Https 설정
  #### 1. [닷네임코리아](https://domain.dotname.co.kr)에서 도메인 구매
  #### 2. [AWS EC2 route53](https://sovovy.tistory.com/37)에서 DNS 등록
  #### 3. [닷네임코리아 네임서버](https://sovovy.tistory.com/37)에 AWS dns 등록
  
  ```html
  www.grnr.co.kr -> 3.38.62.243
  grnr.co.kr -> 3.38.62.243
  ```
  #### 4. aws https에 사용할 443 포트 open
  #### 5. docker-compose.ymal에 https 관련 설정 추가
  ```yml
  # certbot & nginx 설정
  # certbot : 연단위 비용없이 인증서를 발급해주는 서비스로 90일마다 갱신해줘야한다.
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
      #========= https port number =============
      - "443:443" 
    restart: always
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf
      #========= https 인증서 파일 경로=============
      - ./certbot-etc:/etc/letsencrypt
      #========= #nginx root index 파일=============
      - ./myweb:/usr/share/nginx/html
  client:
    restart: always
    build:
      context: /home/ubuntu/greenery-front
      dockerfile: Dockerfile
    container_name: clientcontainer
    depends_on:
      - server

  #========= https 인증서 받기=============
  certbot:
    depends_on:
      - webserver
    image: certbot/certbot
    container_name: certbot
    volumes:
      - ./certbot-etc:/etc/letsencrypt
      - ./myweb:/usr/share/nginx/html # #nginx root index 파일
    command: certonly --dry-run --webroot --webroot-path=/usr/share/nginx/html --email yoho555@icloud.com --agree-tos --no-eff-email --keep-until-expiring -d grnr.co.kr -d www.grnr.co.kr
  #======================

  server:
    restart: restart
    build:
      context: /home/ubuntu/greenery-server/demo
      dockerfile: Dockerfile
    links:
      - "db:greenerydb"
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
      - "3306:3306" # If you don't want to go through nginxproxy and want to approach right away,
    container_name: dbcontainer
  ```
  #### 6. nginx.conf에 https 인증서 받기 위해 추가
  ```conf
      server {
          # ----------------https-----------------
          location ~ /.well-known/acme-challenge {
                  allow all;
                  root /usr/share/nginx/html; #nginx root index 파일
                  try_files $uri =404;
          }

          location / {
                  allow all;
                  root /usr/share/nginx/html; #nginx root index 파일
                  try_files $uri =404;
          }
          # ----------------https-----------------

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
  #### 7. 인증서 발급 확인
  ##### 1. docker-compose up -d 이후 certbot 로그확인
  ```html
  Saving debug log to /var/log/letsencrypt/letsencrypt.log
  Some challenges have failed.
  Ask for help or search for solutions at https://community.letsencrypt.org. See the logfile /var/log/letsencrypt/letsencrypt.log or re-run Certbot with -v for more details.
  Simulating a certificate request for grnr.co.kr and www.grnr.co.kr
  The dry run was successful.
  ```
  ##### 2. dry run successful이므로 docker-compose.yaml에서 dry 옵션 제거 후 실행
  ```yaml
  #========= https 인증서 받기=============
    certbot:
      depends_on:
        - webserver
      image: certbot/certbot
      container_name: certbot
      volumes:
        - ./certbot-etc:/etc/letsencrypt
        - ./myweb:/usr/share/nginx/html
      command: certonly --webroot --webroot-path=/usr/share/nginx/html --email yoho555@icloud.com --agree-tos --no-eff-email --keep-until-expiring -d grnr.co.kr -d www.grnr.co.kr
    #======================
  ```
  ##### 3. ROOT 계정으로 certbot-etc/live 안 인증서 확인
  ```
  # ec2 root 사용자 패스워드 설정 
  $sudo passwd
  # root 사용자로 전환, ubuntu계정으로 돌아올시 : $su - ubuntu
  $su -
  
  ---목록
  path : /home/ubuntu/greenery-db-nginx/certbot-etc/live/grnr.co.kr
   - README  
   - cert.pem  
   - chain.pem  
   - fullchain.pem  
   - privkey.pem
  ```
#### 8. https 으로 통신하기위해 nginx.conf 수정
- 보안파일 
- [options-ssl-nginx.conf](https://raw.githubusercontent.com/certbot/certbot/master/certbot-nginx/certbot_nginx/_internal/tls_configs/options-ssl-nginx.conf)
- [ssl-dhparams.pem](https://raw.githubusercontent.com/certbot/certbot/master/certbot/certbot/ssl-dhparams.pem)
```configure
# http 요청을 모두 https 요청으로 Redirection
server {
        listen 80;
        server_name grnr.co.kr www.grnr.co.kr;

        location ~ /.well-known/acme-challenge {
                allow all;
                root /usr/share/nginx/html; #nginx root index 파일
                try_files $uri =404;
        }

        # Redirection
        location / {
                return 301 https://$host$request_uri;
        }    
    }

server { #https 설정
        listen 443 ssl;
        server_name grnr.co.kr www.grnr.co.kr;
        
        ssl_certificate /etc/letsencrypt/live/grnr.co.kr/fullchain.pem;
        ssl_certificate_key /etc/letsencrypt/live/grnr.co.kr/privkey.pem;

        #certbot-etc 파일에 추가
        include /etc/letsencrypt/options-ssl-nginx.conf; # 보안 강화를 위한 옵션 추가
        ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;   # 보안 강화를 위한 옵션 추가

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
#### 9. docker-compose 실행 후 https://grnr.co.kr 접속
 - cf. certbot 컨테이너는 인증서 확인 후 종료된다.

## 8. log 설정
### [참고](https://ingnoh.tistory.com/50)
- 기본적으로 log-rotation을 지원하지 않으므로, json-file은 계속해서 쌓여 많은 디스크 공간을 차지
- 모든 서비스에 아래와 같이 logging 설정
```yml
version: "3"

services:
  server:
    restart: always
    build:
      context: /home/ubuntu/grnr-server/demo
      dockerfile: Dockerfile
    links:
      - "db:mysqldb"
    ports:
      - "8080:8080" # if want direct access
    container_name: servercontainer
    depends_on:
     - db
    logging:
      options:
        max-size: "1024m" #로그 파일당 최대 용량
        max-file: "5" #로그 파일의 최대 개수
```

## 9. SubDomain 설정
### 1. [route53 subdomain 설정](https://aws.amazon.com/ko/premiumsupport/knowledge-center/create-subdomain-route-53/)
1. route53에서 develop.grnr.co.kr 호스팅 영역 생성, 이때 A 타입 레코드는 기존 grnr.co.kr과 동일한 IP로 설정 
2. 기존 grnr.co.kr 호스팅 영역 에서 레코드 생성
3. 레코드 안 develop 이름으로 1번에서 생성한 ns 값 가져와서 적용
- <img src="https://user-images.githubusercontent.com/60174144/147807723-1da28579-be01-4267-b649-dac250365baf.png" width="70%" height="70%">
### 2. subdomain ssl 적용 [참고1](https://stackoverflow.com/questions/60842065/how-to-add-subdomain-in-letsencrypt-i-am-using-docker-nginx-wordpress)
1. docker-compose 파일에 subdomain [expand](https://blog.naver.com/PostView.nhn?blogId=jodi999&logNo=221753871232)
```yaml
  certbot:
    depends_on:
     - nginxproxy
     - dictionary-client
    image: certbot/certbot
    container_name: certbot
    volumes:
      - ./certbot-etc:/etc/letsencrypt
      - ./myweb:/usr/share/nginx/html
    command: certonly --webroot --webroot-path=/usr/share/nginx/html --email yoho555@icloud.com --agree-tos --no-eff-email --keep-until-expiring -d grnr.co.kr --expand -d develop.grnr.co.kr
```
2. 위 yaml 파일에서 강제 ssl 인증서 갱신
```yaml
# 1. --dry-run 으로 테스트 후 실행
certbot:
  depends_on:
    - nginxproxy
    - dictionary-client
  image: certbot/certbot
  container_name: certbot
  volumes:
    - ./certbot-etc:/etc/letsencrypt
    - ./myweb:/usr/share/nginx/html
  command: certonly --webroot --webroot-path=/usr/share/nginx/html --email yoho555@icloud.com --agree-tos --no-eff-email --keep-until-expiring -d grnr.co.kr --expand -d develop.grnr.co.kr

# 2. docker-compose up --build -d 후 certbot log 확인
  Simulating renewal of an existing certificate for grnr.co.kr and develop.grnr.co.kr
  The dry run was successful.
  Saving debug log to /var/log/letsencrypt/letsencrypt.log
  
# 3. --dry-run 옵션 제거 후 실행
# 4. ./certbot-etc/live 위치에 grnr.co.kr-0001폴더 생성됨
# 5. 기존 grnr.co.kr을 다른 이름으로 변경 후 grnr.co.kr-0001 폴더를 grnr.co.kr 으로 변경
# 6. 만료일 확인하기
 $ sudo openssl x509 -dates -noout < ~/greenery-db-nginx/certbot-etc/live/grnr.co.kr/cert.pem

# 7. nginx.conf 에서 develop.grnr.co.kr 포워딩 설정하기 [생략]
```


## [추후 S3 연동해서 CI/CD](https://github.com/hwangyoungjin/AWS-Docker/tree/main/Second/s3-ci%2Ccd)   

## Reference
[docker-compose 실행후 db 접속시 에러 해결](https://stackoverflow.com/questions/59838692/mysql-root-password-is-set-but-getting-access-denied-for-user-rootlocalhost)
[ec2 업그레이드 - aws 유형 변경](https://nerd-mix.tistory.com/32)
```
* 도커 & workbench에서 접속시 에러
access denied for user 'root'@'localhost'
```