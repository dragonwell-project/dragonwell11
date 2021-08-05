![Dragonwell Logo](https://raw.githubusercontent.com/wiki/alibaba/dragonwell8/images/dragonwell_std_txt_horiz.png)

# Introduction

Over the years, Java has proliferated in Alibaba. Many applications are written in Java and many our Java developers have written more than one billion lines of Java code.

Alibaba Dragonwell, as a downstream version of OpenJDK, is the OpenJDK implementation at Alibaba optimized for online e-commerce, financial, logistics applications running on 100,000+ servers. Alibaba Dragonwell is the engine that runs these distributed Java applications in extreme scaling.

Alibaba Dragonwell is clearly a "friendly fork" under the same licensing terms as the upstream OpenJDK project. Alibaba is committed to collaborate closely with OpenJDK community and intends to bring as many customized features as possible from Alibaba Dragonwell to the upstream.

# Using Alibaba Dragonwell

Alibaba Dragonwell JDK currently supports Linux/x86_64 platform only.

### Installation

* You may download a pre-built Alibaba Dragonwell JDK from its GitHub page:
https://github.com/alibaba/dragonwell11/releases.
* Uncompress the package to the installation directory.

### Enable Alibaba Dragonwell for Java applications

To enable Alibaba Dragonwell JDK for your application, simply set `JAVA_HOME` to point to the installation directory of Alibaba Dragonwell. If you installed Dragonwell JDK via YUM, follow the instructions prompted from post-install outputs, e.g.:

```
=======================================================================
# Assuming Alibaba Dragonwell 11 is installed to:
#    /opt/alibaba/java-11-alibaba-dragonwell
# You can set Alibaba Dragonwell as default JDK by exporting following environment variables:
$ export JAVA_HOME=/opt/alibaba/java-11-alibaba-dragonwell
$ export PATH=${JAVA_HOME}/bin:$PATH
=======================================================================
```

# Acknowledgement

Special thanks to those who have made contributions to Alibaba's internal JDK builds.

# Publications

Technologies included in Alibaba Dragonwell have been published in following papers

## 2021

- Yifei Zhang, Tianxiao Gu, Xiaolin Zheng, Lei Yu, Wei Kuai, Sanhong Li [**Towards a Serverless Java Runtime**](https://ase21-industry.hotcrp.com/doc/ase21-industry-paper7.pdf?cap=07ax_GWBvNW-0U) In _ASE 2021 Industry Showcase_, to appear

## 2020

- Mingyu Wu, Ziming Zhao, Yanfei Yang, Haoyu Li, Haibo Chen, Binyu Zang, Haibing Guan, Sanhong Li, Chuansheng Lu, Tongbao Zhang [**Platinum: A CPU-Efficient Concurrent Garbage Collector for Tail-Reduction of Interactive Services**](https://www.usenix.org/system/files/atc20-wu-mingyu.pdf) In _USENIX ATC 2020_, pp. 159&ndash;172

## 2019

- Shiyou Huang, Jianmei Guo, Sanhong Li, Xiang Li, Yumin Qi, Kingsum Chow, Jeff Huang [**SafeCheck: Safety Enhancement of Java Unsafe API**](https://2019.icse-conferences.org/details/icse-2019-Technical-Papers/96/SafeCheck-Safety-Enhancement-of-Java-Unsafe-API) In _ICSE 2019_, pp. 889&ndash;899

## 2018

- Fangxi Yin, Denghui Dong, Chuansheng Lu, Tongbao Zhang, Sanhong Li, Jianmei Guo, Kingsum Chow [**Cloud-Scale Java Profiling at Alibaba**](https://dl.acm.org/doi/10.1145/3185768.3186295) In _ICPE Companion 2018_, pp. 99&ndash;100

- Fangxi Yin, Denghui Dong, Sanhong Li, Jianmei Guo, Kingsum Chow [**Java Performance Troubleshooting and Optimization at Alibaba**](https://www.icse2018.org/details/icse-2018-Software-Engineering-in-Practice/4/Java-Performance-Troubleshooting-and-Optimization-at-Alibaba) In _ICSE-SEIP 2018_, pp. 11&ndash;12

