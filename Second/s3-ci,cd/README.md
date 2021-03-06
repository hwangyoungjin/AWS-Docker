## 추후 S3 연동해서 CI/CD
  - ### PASS [github action용으로 aws ec2 설정 - fail]
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
      - 권한 : 기존 정책 직접 연결
      - 정책 : CodeDeployFullAccess
      - 사용자 만든 후 생성되는 액세스 키와 비밀번호 키 저장!!

  - ### PASS. github action 설정[아직 해결X]
  - [Reference](https://gist.github.com/jypthemiracle/edf6e92ed10960f3ac2e94fc6fd21a20)
    1. github action 설정
      - server 용 deploy code
      ```
      name: deploy
      on: #main branch에 push하면 event trigger
        push:
          branches:
            - main

      jobs:
        deploy:
          name: Deploy
          runs-on: ubuntu-20.04
          environment: production

          steps:
          - name: Checkout
            uses: actions/checkout@v2
            
          # 메시지를 출력한다.
          - name: Run a one-line script
            run: echo Start server Deploy
            
            # 자바 버전을 설정해준다.
          - name: Set up JDK 1.8
            uses: actions/setup-java@v1
            with:
              java-version: 1.8

            # Gradle에 실행 권한을 부여한다.
          - name: Grant execute permission for gradlew
            run: |
              sudo chmod +x gradlew
            shell: bash
            
            # Gradle을 활용해 배포한다.
          - name: Build with Gradle
            run: |
              sudo ./gradlew clean bootjar
            shell: bash
            
          # AWS 서비스를 사용하기 위한 인증 과정이다.
          - name: Configure AWS credentials
            uses: aws-actions/configure-aws-credentials@v1
            with:
              aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
              aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
              aws-region: ap-northeast-2

          # docker run
          - name: Code Deploy
            run: |
              docker-compose up -d
            shell: bash
            working-directory: /home/ubuntu/greenery-db-nginx
      ```
    - Repo Secret Key 설정
      1. Name : AWS_ACCESS_KEY_ID , Value : 이전에 받은 액세스 ID
      1. Name : AWS_SECRET_ACCESS_KEY , Value : 이전에 받은 액세스 key
      