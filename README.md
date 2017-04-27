自从上次写了《Android检查版本升级应该怎么做？》简书地址：http://www.jianshu.com/p/98ea7e866ffd
后，有很多同学让我提供一份demo。所以就有了UpgradeDemo演示是如何做的检查版本升级。

upgrade包
存放核心的检查升级代码、DownloadManager下载apk文件等。具体大家详细看，有什么不明白的与我交流沟通吧。

Constants类
定义了两个变量，其中API_DOMAIN_DEBUG是测试服务器基地址，API_DOMAIN_RELEASE是正式服务器基地址，大家根据自己的实际情况填写吧。


注意：demo使用的是gradle版本是3.3
     targetSdkVersion是25，如果运行在系统版本在6.0及以上，记得适配权限。