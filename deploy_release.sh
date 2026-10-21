#!/bin/bash
set -e

echo "=========================================="
echo "🚀 نشر تطبيق مكنون - بوابة حلقات القرآن الكريم"
echo "=========================================="

# 1. التأكد من وجود ملف الـ APK
if [ ! -f "./app-debug.apk" ]; then
  if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    cp app/build/outputs/apk/debug/app-debug.apk ./app-debug.apk
  else
    echo "🔨 بناء الـ APK أولاً..."
    gradle assembleDebug
    cp app/build/outputs/apk/debug/app-debug.apk ./app-debug.apk
  fi
fi

# نسخ الـ APK للمجلد العام الخاص بـ Vercel
mkdir -p public
cp ./app-debug.apk public/app-debug.apk

echo "✅ ملف الـ APK جاهز بحجم: $(ls -lh ./app-debug.apk | awk '{print $5}')"

# 2. النشر على Vercel (إذا توفرت أداة vercel CLI)
if command -v vercel &> /dev/null; then
  echo "🌐 جاري النشر على Vercel..."
  vercel --prod --yes || echo "⚠️ يرجى تسجيل الدخول إلى Vercel أولاً عبر 'vercel login'"
else
  echo "ℹ️ لنشر صفحة التحميل على Vercel: قم بتثبيت vercel عبر 'npm i -g vercel' ثم تشغيل 'vercel --prod'"
fi

# 3. النشر على GitHub Release (إذا توفرت أداة gh CLI)
if command -v gh &> /dev/null; then
  echo "📦 جاري إنشاء Release v2.0.0 على GitHub..."
  gh release create v2.0.0 ./app-debug.apk --title "v2.0.0 - تطبيق مكنون لحلقات القرآن" --notes "إصدار رسمي يتضمن المصادقة البيومترية للمشرفين والنسخ الاحتياطي والمزامنة السحابية." || true
else
  echo "ℹ️ لرفع الـ APK على GitHub Releases: قم بتشغيل 'gh release create v2.0.0 ./app-debug.apk --title \"v2.0.0\"'"
fi

echo "=========================================="
echo "🎉 اكتمل التجهيز بنجاح!"
echo "=========================================="
