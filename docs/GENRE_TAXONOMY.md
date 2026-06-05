# Vinfo Genre Taxonomy Contract

**Purpose:** Gemini가 제안한 앨범 장르 후보를 무한히 쌓지 않고, 고정된 Root Genre 안으로 검증해 넣기 위한 장르 사전 계약이다.

---

## 1. Core Principle

Vinfo의 장르 구조는 세 단계로 고정한다.

```text
Root Genre -> Branch Genre -> Micro / Emerging Genre
```

- `Root Genre`: 통계와 지도 안정성을 위한 큰 뿌리 장르.
- `Branch Genre`: 음악적으로 의미 있는 하위 흐름.
- `Micro / Emerging Genre`: Hyperpop, Digicore, Pluggnb처럼 세부적이거나 신생인 장르.

신생 장르는 이름만으로 저장하지 않는다. 반드시 하나 이상의 Root Genre에 연결될 때만 후보로 보존한다.

---

## 2. Fixed Root Genres

MVP의 Root Genre는 아래 8개로 고정한다.

| Root Key | Display Name | 포함 예시 |
|---|---|---|
| `HIP_HOP_RAP` | Hip Hop / Rap | Trap, Boom Bap, Drill, Cloud Rap, Jazz Rap, Southern Hip Hop |
| `RNB_SOUL_BLUES` | R&B / Soul / Blues | Contemporary R&B, Neo-Soul, Soul, Funk, Blues, Gospel |
| `POP` | Pop | Synth-pop, Electropop, Dance-pop, Art Pop, K-Pop, Hyperpop |
| `ROCK` | Rock | Alternative Rock, Indie Rock, Punk, Metal, Shoegaze, Psychedelic Rock |
| `ELECTRONIC` | Electronic | House, Techno, Ambient, IDM, UK Garage, Dubstep, Drum and Bass |
| `JAZZ` | Jazz | Bebop, Fusion, Cool Jazz, Free Jazz, Vocal Jazz, Jazz Rap |
| `CLASSICAL_ORCHESTRAL` | Classical / Orchestral | Classical, Baroque, Romantic, Modern Classical, Film Score |
| `FOLK_COUNTRY_ACOUSTIC` | Folk / Country / Acoustic | Folk, Singer-Songwriter, Country, Americana, Bluegrass, Acoustic |

후속 확장 후보:

| Root Key | Display Name | 사용 조건 |
|---|---|---|
| `GLOBAL_LATIN_AFRO` | Global / Latin / Afro | 라틴, 아프로비트, 레게, 지역 음악 데이터가 충분히 쌓였을 때 |
| `EXPERIMENTAL_AVANT_GARDE` | Experimental / Avant-Garde | 기존 root로 안정적으로 분류하기 어려운 실험 음악이 반복 등장할 때 |

후속 root는 바로 추가하지 않고, `needs_root_review` 후보가 반복적으로 쌓일 때만 도입한다.

---

## 3. Entry Shape

장르 사전의 각 entry는 아래 형태를 따른다.

```kotlin
data class GenreDictionaryEntry(
    val genreKey: String,
    val displayName: String,
    val aliases: List<String>,
    val root: RootGenreKey,
    val secondaryRoots: List<RootGenreKey>,
    val branch: String?,
    val status: GenreEntryStatus,
    val relations: List<GenreRelation>
)

enum class GenreEntryStatus {
    VERIFIED,
    EMERGING,
    NEEDS_REVIEW
}
```

예시:

```json
{
  "genre_key": "TRAP",
  "display_name": "Trap",
  "aliases": ["Trap Rap", "Trap Music"],
  "root": "HIP_HOP_RAP",
  "secondary_roots": [],
  "branch": "Southern Hip Hop",
  "status": "VERIFIED",
  "relations": [
    { "to": "SOUTHERN_HIP_HOP", "type": "DERIVED", "curated": true },
    { "to": "HIP_HOP_RAP", "type": "ROOT", "curated": true }
  ]
}
```

```json
{
  "genre_key": "DIGICORE",
  "display_name": "Digicore",
  "aliases": ["Digi-core"],
  "root": "POP",
  "secondary_roots": ["HIP_HOP_RAP", "ELECTRONIC"],
  "branch": "Digital Pop / Internet Rap",
  "status": "EMERGING",
  "relations": [
    { "to": "HYPERPOP", "type": "ADJACENT", "curated": true },
    { "to": "CLOUD_RAP", "type": "ADJACENT", "curated": true }
  ]
}
```

---

## 4. Normalization Rules

Gemini 응답은 아래 순서로 처리한다.

1. `primary_genres`, `secondary_genres`, `microgenres`를 모두 후보 배열로 합친다.
2. 후보 이름을 trim/lowercase/기호 정리 후 alias 사전에 매칭한다.
3. 사전에 있으면 해당 `genreKey`를 사용한다.
4. 사전에 없으면 Root Classification을 시도한다.
5. Root confidence가 기준 이상이면 `EMERGING` 또는 `NEEDS_REVIEW` 후보로 저장한다.
6. Root confidence가 기준 미만이면 저장하지 않고 `ignored_genres` 또는 `reliability_notes`에 남긴다.

Threshold:

| 단계 | 기준 |
|---|---|
| Verified 장르 활성화 | candidate confidence `>= 0.80` |
| Emerging 후보 저장 | candidate confidence `>= 0.70` and root confidence `>= 0.65` |
| 지도 표시 | `VERIFIED`만 기본 표시, `EMERGING`은 사용자가 후보 표시를 켰을 때만 흐린 노드 |
| 통계 집계 | Root 통계에는 `VERIFIED`와 승인된 `EMERGING`만 포함 |

---

## 5. Emerging Genre Policy

신생 장르는 무제한으로 쌓지 않는다.

- Root가 없으면 저장하지 않는다.
- Root가 2개 이상이면 `root` 하나와 `secondaryRoots` 여러 개로 나눈다.
- 같은 alias가 3회 이상 반복되고 root 분류가 일관되면 `NEEDS_REVIEW`에서 `EMERGING`으로 승격 가능하다.
- 사용자가 승인하거나 개발자가 사전에 추가하면 `VERIFIED`로 승격한다.
- `EMERGING`은 기본 장르 통계의 세부 장르 순위에는 표시하지 않고, root 비율에는 약하게 반영하거나 별도 후보 섹션에 둔다.

예시:

```text
Hyperpop
root: POP
secondaryRoots: ELECTRONIC
status: VERIFIED
```

```text
Digicore
root: POP
secondaryRoots: HIP_HOP_RAP, ELECTRONIC
status: EMERGING
```

```text
Unknown Internet Post-Genre
root confidence < 0.65
status: ignored
```

---

## 6. Map And Stats Behavior

지도:

- Root Genre는 큰 배경 축 또는 클러스터 중심으로 사용한다.
- Verified Branch/Micro 장르는 실제 노드로 표시한다.
- Emerging 장르는 기본 화면에서는 숨기고, 선택 장르 주변 후보가 활성화되었을 때만 흐린 후보 노드로 표시한다.
- AI가 장르 간 관계선을 새로 만들 수 없다. 관계선은 사전 또는 사용자 승인된 주변 검색 결과만 사용한다.

통계:

- 기본 비율은 Root Genre 기준으로 집계한다.
- 세부 장르 비율은 Root 안에서 drill-down 방식으로 표시한다.
- 예: Trap은 `HIP_HOP_RAP` 비율에 포함되지만, 세부 장르에서는 `Trap`으로 남는다.

```text
Root 통계:
Hip Hop / Rap 42%
Pop 21%
Electronic 14%

Hip Hop / Rap 세부:
Trap 38%
Boom Bap 19%
Jazz Rap 12%
```

---

## 7. Implementation Targets

문서 계약을 코드로 옮길 때의 우선순위:

1. `RootGenreKey`, `GenreEntryStatus` 모델 추가.
2. `GenreDictionary`를 버전 관리되는 JSON 또는 Kotlin 리소스로 분리.
3. `NormalizeAlbumGenreCandidatesUseCase`에서 alias 매칭, root 검증, emerging 후보 필터링 수행.
4. `GetVisibleGenreFlowUseCase`에서 root/verified/emerging 표시 정책을 적용.
5. Archive/Stats는 root 집계와 세부 장르 drill-down을 분리한다.

---

## 8. Non-goals

- Gemini가 새 root genre를 자동 생성하지 않는다.
- Gemini가 장르 영향선을 즉석 생성하지 않는다.
- 사전에 없고 root 검증도 실패한 장르는 지도와 통계에 표시하지 않는다.
- 모든 microgenre를 즉시 정식 장르로 승격하지 않는다.
