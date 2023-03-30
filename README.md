# pangolin-java


### ubuntu系统上部署前提需要安装java环境：
```shell
sudo apt update
sudo apt install openjdk-17-jdk
java -version
```

### maven打包上传到服务器部署：

```shell
mvn clean package
scp path/to/your/client.jar user@serverA_IP:/path/to/destination
scp path/to/your/config.json user@serverA_IP:/path/to/destination
java -jar /path/to/client.jar

scp path/to/your/server.jar user@serverB_IP:/path/to/destination
scp path/to/your/config.json user@serverA_IP:/path/to/destination
java -jar /path/to/server.jar
```


