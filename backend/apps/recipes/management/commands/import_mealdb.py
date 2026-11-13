import io
import re
import time
from pathlib import Path

import requests
from django.conf import settings
from django.core.management.base import BaseCommand
from django.utils.text import slugify
from PIL import Image

from apps.recipes.models import Area, Category, Recipe, RecipeIngredient

API = "https://www.themealdb.com/api/json/v1/{key}/"

def split_steps(instructions):
    text = re.sub(r"(?im)^\s*(?:step\s*)?\d+\s*[.:)\-]?\s*", "", instructions or "")
    parts = [re.sub(r"\s+", " ", part).strip() for part in re.split(r"\r?\n+", text)]
    return [part for part in parts if len(part) > 8] or ([text.strip()] if text.strip() else [])

class Command(BaseCommand):
    help = "Import recipe data and images from TheMealDB into Simmer"

    def add_arguments(self, parser):
        parser.add_argument("--limit", type=int)
        parser.add_argument("--skip-images", action="store_true")
        parser.add_argument("--only-new", action="store_true")

    def handle(self, *args, **options):
        key = __import__("os").environ.get("THEMEALDB_API_KEY", "1")
        base = API.format(key=key)
        session = requests.Session()
        session.headers["User-Agent"] = "Simmer Recipe Importer/0.1 (respectful API client)"
        categories = session.get(base + "categories.php", timeout=30).json().get("categories") or []
        for item in categories:
            Category.objects.update_or_create(external_id=item["idCategory"], defaults={
                "name": item["strCategory"], "slug": slugify(item["strCategory"]),
                "thumbnail": item.get("strCategoryThumb") or "", "description": item.get("strCategoryDescription") or ""})
        areas = session.get(base + "list.php?a=list", timeout=30).json().get("meals") or []
        for item in areas:
            name = item.get("strArea")
            if name: Area.objects.update_or_create(name=name, defaults={"slug": slugify(name)})
        total, saved, skipped, failed = 0, 0, 0, 0
        media_root = Path(settings.MEDIA_ROOT)
        for letter in "abcdefghijklmnopqrstuvwxyz":
            meals = session.get(base + f"search.php?f={letter}", timeout=30).json().get("meals") or []
            for item in meals:
                if options["limit"] and total >= options["limit"]: break
                total += 1
                try:
                    title = (item.get("strMeal") or "").strip()
                    instructions = (item.get("strInstructions") or "").strip()
                    steps = split_steps(instructions)
                    image_url = (item.get("strMealThumb") or "").strip()
                    ingredients = [(item.get(f"strIngredient{i}") or "").strip() for i in range(1, 21)]
                    if not title or not instructions or not image_url or not any(ingredients):
                        skipped += 1
                        continue
                    cat_name = (item.get("strCategory") or "Miscellaneous").strip()
                    category, _ = Category.objects.get_or_create(name=cat_name, defaults={"external_id": f"local-{slugify(cat_name)}", "slug": slugify(cat_name)})
                    # Recent V1 records can leave strArea empty while returning strCountry.
                    area_name = (item.get("strArea") or item.get("strCountry") or "").strip()
                    area = Area.objects.filter(name=area_name).first() if area_name else None
                    recipe, created = Recipe.objects.get_or_create(external_id=item["idMeal"], defaults={
                        "title": title, "slug": slugify(title)[:220], "category": category, "area": area,
                        "instructions": instructions, "steps": steps, "is_published": len(steps) >= 2, "image": image_url,
                        "thumbnail": image_url, "tags": [x.strip() for x in (item.get("strTags") or "").split(",") if x.strip()],
                        "youtube_url": item.get("strYoutube") or "", "source_url": item.get("strSource") or ""})
                    if not created and options["only_new"]:
                        skipped += 1
                        continue
                    if not created and not options["only_new"]:
                        recipe.title, recipe.slug, recipe.category, recipe.area = title, slugify(title)[:220], category, area
                        recipe.instructions, recipe.steps = instructions, steps
                        recipe.is_published = len(steps) >= 2
                        recipe.image = recipe.thumbnail = image_url
                        recipe.tags = [x.strip() for x in (item.get("strTags") or "").split(",") if x.strip()]
                        recipe.youtube_url, recipe.source_url = item.get("strYoutube") or "", item.get("strSource") or ""
                        recipe.save()
                    if not options["skip_images"]:
                        response = session.get(image_url, timeout=45)
                        response.raise_for_status()
                        image = Image.open(io.BytesIO(response.content)).convert("RGB")
                        folder = media_root / "recipes" / recipe.external_id
                        folder.mkdir(parents=True, exist_ok=True)
                        paths = []
                        for width in (400, 1000):
                            copy = image.copy()
                            copy.thumbnail((width, width), Image.Resampling.LANCZOS)
                            target = folder / f"{width}.webp"
                            copy.save(target, "WEBP", quality=82, method=6)
                            paths.append(target.relative_to(media_root).as_posix())
                        recipe.thumbnail, recipe.image = f"/media/{paths[0]}", f"/media/{paths[1]}"
                        recipe.save(update_fields=["thumbnail", "image"])
                    RecipeIngredient.objects.filter(recipe=recipe).delete()
                    RecipeIngredient.objects.bulk_create([
                        RecipeIngredient(recipe=recipe, position=i, name=name,
                            measure=(item.get(f"strMeasure{i}") or "").strip())
                        for i, name in enumerate(ingredients, start=1) if name])
                    saved += 1
                except Exception as exc:
                    failed += 1
                    self.stderr.write(f"{item.get('idMeal', '?')} {item.get('strMeal', '?')}: {exc}")
                time.sleep(0.25)
            if options["limit"] and total >= options["limit"]: break
        self.stdout.write(self.style.SUCCESS(f"Scanned {total}; imported {saved}; skipped {skipped}; failed {failed}"))
