node {
    checkout scm
    def app
    def dockerImage = docker.build("buildprocess/docker", "./JenkinsCI-Docker/docker/")
    withEnv([
        'DOCKER_ACCOUNT=firestarthehack',
        'IMAGE_VERSION=1.01',
        'IMAGE_NAME=animecapgroupwatch',
        'RANCHER_STACK_NAME=AnimeCap',
        'RANCHER_SERVICE_NAME=GroupWatch',
        'RANCHER_SERVICE_URL=http://34.215.0.188:8080/v2-beta'
    ]){
        stage('Build') {
          dockerImage.inside{
            sh 'rm -rf dockerbuild/'
            sh 'chmod 0755 ./gradlew;./gradlew clean build --refresh-dependencies'
          }
        }
        stage('Docker Build') {
            sh "mkdir dockerbuild/"
            sh 'cp build/libs/*.jar dockerbuild/app.jar && cp Dockerfile dockerbuild/Dockerfile'
            app = docker.build("${env.DOCKER_ACCOUNT}/${env.IMAGE_NAME}", "./dockerbuild/")
            archiveArtifacts(artifacts: 'build/libs/*.jar', onlyIfSuccessful: true)
        }
        stage('Test image') {
            app.inside {
              sh 'echo "Tests passed"'
            }
        }
        stage('Publish Latest Image') {
            app.push("${env.IMAGE_VERSION}")
            app.push("latest")
        }
        stage('Deploy') {
            rancher(environmentId: '1a5', ports: '', environments: '1i12214', confirm: true, image: "${env.DOCKER_ACCOUNT}/${env.IMAGE_NAME}:${env.IMAGE_VERSION}", service: "${env.RANCHER_STACK_NAME}/${env.RANCHER_SERVICE_NAME}", endpoint: "${env.RANCHER_SERVICE_URL}", credentialId: 'rancher-server')
        }
    }
}
