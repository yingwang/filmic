# Filmic

Android photo styling app — applies color grades inspired by **Hasselblad**, **Fujifilm**, and **Leica** to photos from the gallery.

## Status

Initial scaffold. Core flow works end-to-end:

1. Pick a photo from the gallery (PhotoPicker)
2. Browse 5 built-in styles grouped by brand
3. Preview with live re-processing, switch styles via chip row
4. Export to `Pictures/Filmic/` in the gallery

## Styles

| Brand | Name | Description |
| --- | --- | --- |
| Hasselblad | Natural Colour | 清雅、肤色准、高光从容 |
| Fujifilm | Classic Chrome | 灰调、抬影、青橙分离 |
| Fujifilm | Velvia | 浓郁、反差大、绿红尤盛 |
| Leica | Standard | 克制、微暖、层次见内 |
| Leica | Monochrom | 黑白、深黑、银盐颗粒 |

These are hand-tuned `ColorMatrix` + tone-curve approximations, not calibrated LUTs. A `.cube` loader and calibrated profiles are on the roadmap.

## Stack

- Kotlin 2.0 · Compose BOM 2024.09
- Material 3, Navigation Compose
- Coil for gallery thumbnails
- `android.graphics.ColorMatrix` + CPU tone-curve for the color engine

## Build

```
./gradlew :app:assembleDebug
```

Requires Android Studio Koala / Gradle 8.9+ / JDK 17.

## Roadmap

- [ ] `.cube` 3D LUT loader (trilinear interpolation on GPU shader)
- [ ] Per-brand LUT pack calibrated against real sample pairs
- [ ] Grain engine with brand-specific profiles (not just monochrome)
- [ ] Split-toning, curves, HSL panels
- [ ] Batch export
- [ ] RAW (.dng) support
