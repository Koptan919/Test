# Noor content packs

Noor never ships religious texts it cannot verify. Quranic text is bundled from
the Tanzil Project and checked with a SHA-256 checksum at runtime. The texts of
long supplications and ziyarat (Du'a Kumayl, Ziyarat Ashura, …) are **not**
bundled. Each catalogue entry shows an "unavailable" state until you import a
content pack prepared from a source you trust, such as a published edition of
*Mafatih al-Jinan*.

Import a pack in **Settings → Advanced settings → Content synchronisation → Import content pack**. It is stored
privately in the app's files directory and can be removed at any time.

## Format

```json
{
  "format": "noor-content-pack",
  "version": 1,
  "name": "My verified texts",
  "source": "Mafatih al-Jinan, <publisher>, <year>, ISBN …",
  "items": [
    {
      "id": "kumayl",
      "source": "Mafatih al-Jinan, p. …",
      "text": [
        "First paragraph or line of the supplication",
        "Second paragraph …"
      ]
    }
  ]
}
```

Rules enforced by the importer:

* `format` must be `noor-content-pack` and `source` must not be empty.
* Only `id`s that exist in the catalogue are accepted (others are ignored):
  `kumayl`, `tawassul`, `nudba`, `ahd`, `faraj`, `sabah`, `iftitah`,
  `abu_hamza`, `arafah`, `mashlool`, `jawshan_kabir`, `makarim`, `ashura`,
  `warith`, `aminullah`, `jamia_kabira`, `arbaeen`, `al_yasin`, `abbas`,
  `fatima`, `munajat_15`, `munajat_shabaniyya`.
* Each accepted item needs a non-empty `text` array. Every element is shown
  as its own paragraph, in order.
* Quranic supplications and the Tasbih al-Zahra cannot be overridden: they
  always come from the verified Quran text or the bundled formula.
* **New entries** (chapters of books) may be added in the categories `sahifa`,
  `nahj`, `supplications`, `ziyarat` and `munajat`. Each one needs a new `id`,
  a `category`, a `titleAr` (and optionally `titleEn`), and its `text`:

  ```json
  {"id": "nahj_sermon_1", "category": "nahj", "titleAr": "الخطبة ١", "titleEn": "Sermon 1",
   "source": "Nahj al-Balagha, <edition>, p. …", "text": ["…"]}
  ```

## Distribution

Approved packs are published as `content-*` releases in the public release
repository, together with a `.sha256` file. See `RELEASING.md`. The app verifies
the checksum and structure before installing a pack, and it never replaces a pack
the user imported themselves.

The catalogue itself lives in
`core/src/main/kotlin/com/noor/core/content/Content.kt`.
