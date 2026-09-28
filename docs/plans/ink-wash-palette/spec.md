# 수묵화 팔레트 — 무엇을 만드는가

## 범위
- `ref.palette` 를 수묵화 스케일로 교체 (paper · wash200~950 · slate200~900 · red 유지)
- `sys.color` 에 Material 3 색 역할 29개 추가, 기존 차폐·떠 있는 표면 역할은 새 팔레트로 재지정
- 생성·빌드·lint

## 완료 조건
- [x] `build.py --check` 일치 (251 토큰)
- [x] `assembleDebug` · `lintDebug` 0 errors
- [x] 글자·표면 쌍 대비 — 텍스트 4.5:1 이상, 비텍스트 3:1 이상 (아래 표)

| 쌍 | 대비 |
|---|---|
| paper / black (차폐 글자) | 20.7 |
| onSurface / surface | 17.8 |
| onSurfaceVariant / surface | 11.2 |
| primary / surface | 8.7 |
| onPrimary / primary | 7.4 |
| onPrimaryContainer / primaryContainer | 7.9 |
| onSecondaryContainer / secondaryContainer | 9.0 |
| onErrorContainer / errorContainer | 5.9 |
| paper / accentPositive (칩 라벨) | 6.7 |
| paper / red500 (칩 라벨) | 5.4 |
| wash300 / wash700 (버블 기호) | 5.5 |
| outline / surface (비텍스트) | 4.3 |
