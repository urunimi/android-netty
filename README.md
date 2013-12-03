netty-android
=============

<b>Introduction</b>
 
이 라이브러리는 Netty(http://netty.io/) 프로젝트 v3.8을 Android 용으로 경량화한 버전입니다. <br/>
별도의 소스코드 추가없이 Netty 내부의 코드에서 Android에 불필요한 내용을 삭제하고 정리했습니다. <br/>
Copyright 및 License는 Netty 프로젝트와 동일합니다.

Android Framework은 Java SE 기반이 아니기 때문에 SE 기반으로 작성된 Netty java 라이브러리에서 사용하지 못하는 API가 많습니다. 

이런 코드들을 제거했고 용량에 민감한 Android 개발 특성을 만족시켰습니다. <br/>
특히 TCP 통신 기반의 클라이언트 개발을 하신다면 사용해 보십시오.

Jar 파일 용량 기준 279 KB 정도로 기존 1.2 MB에 비해 1/3 도 안되는 크기 이며 Proguard 적용시 50 ~ 100 KB 미만으로 줄일 수 있습니다.


<b>License</b>
 
 Copyright 2012 The Netty Project
 
 The Netty Project licenses this file to you under the Apache License,
 version 2.0 (the "License"); you may not use this file except in compliance
 with the License. You may obtain a copy of the License at:
 
   http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 License for the specific language governing permissions and limitations
 under the License.
