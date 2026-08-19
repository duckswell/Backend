<div align="center">
  <strong><img width="1920" height="1080" alt="중커톤 표지" src="https://github.com/user-attachments/assets/f4627886-5d58-4ac4-a803-079f3d24fd37" /></strong>
</div>

---

## 🧴 HALE 프로젝트 개요

### 서비스 소개
시술 정보와 매일의 피부 상태를 AI로 분석해, 시술 후 매일 달라지는 피부에 맞춘 오늘의 회복 루틴을 제공하는 서비스

> **개발 기간**: 2026.07.26 ~ 2026.08.21

---

## 👥 백엔드 팀원 소개

<table align="center">
  <thead>
    <tr>
      <th>이수진</th>
      <th>김나은</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td align="center"><img src="https://avatars.githubusercontent.com/leewatertrue" alt="이수진님 사진" width="200" height="200"></td>
      <td align="center"><img src="https://avatars.githubusercontent.com/naeuun" alt="김나은님 사진" width="200" height="200"></td>
    </tr>
    <tr>
      <td align="center"><a href="https://github.com/leewatertrue">@leewatertrue</a></td>
      <td align="center"><a href="https://github.com/naeuun">@naeuun</a></td>
    </tr>
    <tr>
      <td valign="top">
        <ul>
          <li>초기 프로젝트 세팅</li>
          <li>계정 및 ERD 설계</li>
          <li>AI 모델 및 API</li>
          <li>집중 코스 케어</li>
          <li>데일리 코스 케어</li>
        </ul>
      </td>
      <td valign="top">
        <ul>
          <li>배포 환경 세팅</li>
          <li>AI API</li>
          <li>홈 화면</li>
          <li>마이페이지</li>
          <li>제품 추천 상점</li>
        </ul>
      </td>
    </tr>
  </tbody>
</table>

---

## ⚙️ 기술 스택

<div align="center">
<table width="100%">
<tr>
<th align="center">Backend</th>
<td align="left">
<img src="https://skillicons.dev/icons?i=java,spring,gradle,idea" alt="Java, Spring Boot, Gradle, IntelliJ">
</td>
</tr>
<tr>
<th align="center">Database/Docs</th>
<td align="left">
<img src="https://skillicons.dev/icons?i=mysql" alt="MySQL">
<img height="40" src="https://raw.githubusercontent.com/marwin1991/profile-technology-icons/refs/heads/main/icons/swagger.png" title="Swagger">
</td>
</tr>
<tr>
<th align="center">AI (duckswell/AI 레포)</th>
<td align="left">
<img src="https://skillicons.dev/icons?i=opencv,sklearn" alt="OpenCV, scikit-learn">
<img height="40" alt="mediapipe-Photoroom" src="https://github.com/user-attachments/assets/32beba05-22f2-48be-baa1-d87abe84e1ac" />
<img src="https://raw.githubusercontent.com/marwin1991/profile-technology-icons/refs/heads/main/icons/pandas.png" alt="pandas" height="40"> 
<img src="https://raw.githubusercontent.com/marwin1991/profile-technology-icons/refs/heads/main/icons/numpy.png" alt="NumPy" height="40">
</td>
</tr>
<tr>
<th align="center">Collaboration</th>
<td align="left">
<img src="https://skillicons.dev/icons?i=git,github,figma,notion" alt="Git, GitHub, Figma, Notion">
</td>
</tr>
<tr>
<th align="center">CI/CD, External API</th>
<td align="left">
<img src="https://skillicons.dev/icons?i=githubactions" alt="GitHub Actions">
<img height="40" alt="weather" src="https://github.com/user-attachments/assets/c864d529-7b78-4a67-8999-61248f374b7b" />
<img height="40" alt="openai 1" src="https://github.com/user-attachments/assets/cf786f9f-860f-480b-a28e-bab97f7ad2fd" />
</td>
</tr>
<tr>
<th align="center">Deployment</th>
<td align="left">
<img src="https://skillicons.dev/icons?i=docker,aws,nginx" alt="Docker, AWS(S3), nginx">
<img height="40" alt="gabia" src="https://github.com/user-attachments/assets/a5cca70b-891f-489b-aa07-157c2dffe549" />
<img height="50" alt="s3-Photoroom" src="https://github.com/user-attachments/assets/d115a3e4-8782-4660-b970-1022b86eaf89" />
</tr>
</table>
</div>

---

## 🧩 서버 아키텍처
<img width="1775" height="1154" alt="Frame 3 (2)" src="https://github.com/user-attachments/assets/90f9c4ec-ece5-49d4-852c-1ccf0c154604" />

---

## 🗂️ ERD
[![duckswell.png](https://i.postimg.cc/jjgjZRnk/duckswell.png)](https://postimg.cc/xkmYdDRK)

---

## 🌿 브랜치 전략 & 커밋 컨벤션

### 🌱 브랜치 구조
`develop` 없이, **GitHub Flow** 기반의 구조로 운영합니다.

```
main                 배포 가능한 상태만 유지 (직접 push 금지)
 ├─ feat/…           새로운 기능 개발
 ├─ fix/…            개발 중 발견된 버그 수정
 └─ hotfix/…         배포 이후 긴급 수정
```

### 🏷️ 브랜치 네이밍
| Prefix | 용도 | 예시 |
|:---:|---|---|
| `main` | 배포용 브랜치 (직접 건드리지 않음) | `main` |
| `feat/` | 새로운 기능 개발 | `feat/12/login-api` |
| `fix/` | 개발 중 버그 수정 | `fix/8/typo-correction` |
| `hotfix/` | 배포 후 긴급 수정 | `hotfix/3/server-down` |


### ✏️ 커밋 컨벤션

| 이모지 | 타입 | 의미 |
|:---:|---|---|
| 🎉 | `start` | 프로젝트 초기 세팅 |
| ✨ | `feat` | 새로운 기능 추가 |
| 🐛 | `fix` | 버그 수정 |
| 🎨 | `design` | UI/CSS 등 디자인 변경 |
| ♻️ | `refactor` | 코드 리팩토링 |
| 🔧 | `settings` | 설정 파일 변경 |
| 🗃️ | `comment` | 주석 추가·변경 |
| ➕ | `dependency` | 의존성/플러그인 추가 |
| 📝 | `docs` | 문서 수정 |
| 🔀 | `merge` | 브랜치 병합 |
| 🚀 | `deploy` | 배포 관련 작업 |
| 🚚 | `rename` | 파일·폴더명 이동/수정 |
| 🔥 | `remove` | 파일 삭제 |
| ⏪ | `revert` | 이전 버전으로 롤백 |
| ✅ | `test` | 테스트 코드 작성 |

