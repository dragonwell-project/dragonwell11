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

* ICSE'19：https://2019.icse-conferences.org/event/icse-2019-technical-papers-safecheck-safety-enhancement-of-java-unsafe-api

* ICPE'18: https://dl.acm.org/citation.cfm?id=3186295

* ICSE'18 SEIP  https://www.icse2018.org/event/icse-2018-software-engineering-in-practice-java-performance-troubleshooting-and-optimization-at-alibaba
