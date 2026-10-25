# Volna Lab 0.14: исследование SoundCloud-клиентов и частичное исправление

## Короткий результат
Найден и исправлен конкретный путь сбоя подготовки запросов аккаунта: 0.13 использует мобильный User-Agent WebView, но ожидает конфигурацию настольного сайта. Публичный анонимный HTTP-тест воспроизводит перенаправление на m.soundcloud.com без `__sc_hydration/apiClient`. User-Agent, сформированный новым Java-кодом, получает настольную страницу с корректным по формату apiClient.

Это НЕ доказательство успешного лайка/создания плейлиста в аккаунте пользователя. Живые записи в пользовательском аккаунте и работа нового APK на устройстве не проверены. Отдельные 401/403, проверки сервиса, сетевые ограничения или изменения API могут оставаться. Покупать Artist Pro для проверки этого исправления не требуется.

## Что действительно изучено
Составлен список всех 41 записей; выполнен поиск по отобранным README/исходникам трёх страниц указанной подборки `soundcloud-client`, отсортированной по обновлению. Скрипт отбора сохранил 754 файлов; дополнительно вручную прочитан Quantm/main.js. Предметно разобраны механизмы Resona, Jomvet и Quantm; количество сохранённых файлов не означает построчное прочтение каждого. Это ограниченное исследование релевантных файлов, не полный аудит всех файлов всех репозиториев, не проверка работоспособности каждой сборки и не «весь GitHub». Некоторые записи содержат только релизные ссылки/README, а не реализацию. Скрипты и сборки чужих проектов не запускались.

Дополнительно прочитаны SoundcloudStreamExtractor.java и SoundcloudParsingHelper.java проекта TeamNewPipe/NewPipeExtractor, soundcloud.py проекта yt-dlp/yt-dlp, README soundnode/soundnode-app. Запрос README SoundCleod/soundcleod вернул 404; этот проект не считается изученным.

## Конкретные находки
- Resona запускает браузерные запросы like/unlike через прикреплённый WebView с настольным UA. Сам путь `users/{userId}/track_likes/{trackId}` совпадает с Volna; простая замена маршрута не объясняет найденный сбой загрузки страницы. Источник: https://github.com/savo-o/resona/blob/0adbfd121e049c9cf9fbed08d3a32f51d73edf4d/app/src/main/java/com/savoo/scclient/data/remote/WebViewApiBridge.kt.
- Jomvet использует реальный браузерный контекст с постоянной сессией, размером окна 520×680 и собственным cookie авторизации. В 0.14 исправлены режим сайта и размер фонового WebView; полное копирование транспорта Jomvet, подмена защитных cookies и автоматическое прохождение проверок не выполнялись. Источник: https://github.com/Jomvet-lovers/soundcloud-desktop/blob/248943cfc440a375f8de40c7d40df6ccc8f9930b/desktop/src-tauri/src/direct/webview/mod.rs.
- Quantm использует Chromium/Electron для запросов, обновляет публичный client_id, разделяет повтор чтения и небезопасный повтор записи. Оригинал для офлайна получает только после проверки разрешения автора и оставшегося лимита загрузок. Источники: https://github.com/quantm-app/Quantm/blob/7c96c47a384db9192822a2c5980a6b1ae809d626/main.js и https://github.com/quantm-app/Quantm/blob/7c96c47a384db9192822a2c5980a6b1ae809d626/offline.js.
- NewPipe различает возможность потокового воспроизведения и скачивания оригинала; пропускает DRM-протоколы в исследованном экстракторе. Наличие полного потока не подтверждает разрешение на его офлайн-сохранение. Источник: https://github.com/TeamNewPipe/NewPipeExtractor/blob/HEAD/extractor/src/main/java/org/schabi/newpipe/extractor/services/soundcloud/extractors/SoundcloudStreamExtractor.java .
- Часть проектов обращается к собственному backend. Его маршруты, чужие ключи и заявления README нельзя выдавать за официальный интерфейс SoundCloud или за доказанное право на скачивание.

## Изменения 0.14
1. Настольный режим для фонового транспорта операций, входа и явно открываемой страницы проверки. Версия реального установленного Chromium сохраняется, фиксированная выдуманная версия Chrome не вставляется.
2. Фоновый WebView прикреплён за нативным интерфейсом с полноценным размером вместо 1×1; он не открывает страницу лайкнутых по нажатию сердечка. Проверка сервиса по-прежнему открывается только отдельным явным действием пользователя.
3. Различаются незагруженная страница, отсутствие API-конфигурации, HTTP-ошибка страницы, TLS, неопределённый результат запроса. Диалог содержит ограниченную диагностику без URL, client_id, заголовков и токенов: этап, категория сайта, готовность, счётчик API-запросов, коды WebView и его версия.
4. Ошибка локального сохранения сессии после принятой сервером записи больше не оставляет операцию без callback: показывается неопределённый результат, без слепого повтора.
5. Сохранены проверка владельца через /me, отсутствие слепого повтора записи и запрет показывать ложный успешный лайк.

## Что НЕ исправлено и почему это важно
- `NO_OFFLINE_SOURCE` генерируется локально OfflineStore, когда нет разрешённого оригинала или поддерживаемого лицензированного офлайн-источника. Это НЕ буквальный HTTP-ответ SoundCloud и НЕ доказательство, что трек нельзя слушать онлайн. Старое сообщение было недостаточно точным.
- В 0.14 этот отбор источников не снят, проблемные треки не объявлены скачиваемыми и не заменены превью. Обход DRM, платного доступа или явного запрета скачивания, скрытая запись таких потоков не реализованы.
- Повтор/докачка разрешённых оригиналов, переключатель автозагрузки, сохранение уже готовых файлов, сохранение авторизации при холодном старте и сброс очереди воспроизведения из 0.13 не менялись.
- Нативный локальный сетевой маршрут AppNetwork/DpiTunnel не применяется автоматически к Chromium WebView. Если сайт на устройстве недоступен только в WebView, настольный режим не исправляет эту сетевую проблему. В диагностике она теперь видна отдельно. Небезопасный обход проверки сертификатов не добавлялся.

## Как проверить на телефоне
Установить APK поверх 0.13, не удаляя приложение и не очищая данные. Проверить лайк → дождаться подтверждения → проверить то же состояние на сайте SoundCloud; затем снять лайк. Создать приватный тестовый плейлист и добавить в него трек, проверить состав на сайте. При ошибке прислать код и строку диагностики из диалога; никаких cookies или токенов не нужно. Не повторять вслепую создание плейлиста при UNKNOWN_OUTCOME.

## Охват каждой записи подборки
Число файлов — результат ограниченного отбора по именам/размеру; это не оценка качества проекта.

| Репозиторий | Отобрано файлов |
|---|---:|
| [savo-o/resona-status](https://github.com/savo-o/resona-status) | 1 |
| [polishtinsmithabyss/SoundCloud-Downloader-2026](https://github.com/polishtinsmithabyss/SoundCloud-Downloader-2026) | 1 |
| [zxcloli666/SoundCloud-Desktop](https://github.com/zxcloli666/SoundCloud-Desktop) | 131 |
| [depado/parakeet](https://github.com/depado/parakeet) | 2 |
| [Jomvet-lovers/soundcloud-desktop](https://github.com/Jomvet-lovers/soundcloud-desktop) | 47 |
| [quantm-app/Quantm](https://github.com/quantm-app/Quantm) | 10 |
| [omkarcloud/soundcloud-scraper](https://github.com/omkarcloud/soundcloud-scraper) | 17 |
| [luviel19/Music_Island](https://github.com/luviel19/Music_Island) | 3 |
| [wubly/mosaic](https://github.com/wubly/mosaic) | 15 |
| [savo-o/resona](https://github.com/savo-o/resona) | 60 |
| [COMF2222/veirune](https://github.com/COMF2222/veirune) | 18 |
| [okeydw/SoundCloud-Android](https://github.com/okeydw/SoundCloud-Android) | 29 |
| [Orang-Studio/OrangPlayer](https://github.com/Orang-Studio/OrangPlayer) | 1 |
| [zxczxczxczxczxczxc1111/soundcloud-desktop](https://github.com/zxczxczxczxczxczxc1111/soundcloud-desktop) | 3 |
| [dwindles/holdonquietly-soundcloud](https://github.com/dwindles/holdonquietly-soundcloud) | 2 |
| [GraspCommercial76/soundcloud-pc-app](https://github.com/GraspCommercial76/soundcloud-pc-app) | 2 |
| [end-sky/wisp](https://github.com/end-sky/wisp) | 3 |
| [Fleacon/SoundCloudSharp](https://github.com/Fleacon/SoundCloudSharp) | 134 |
| [alexandre-abrioux/soundcloud-reverse](https://github.com/alexandre-abrioux/soundcloud-reverse) | 7 |
| [BlazeInferno64/soundcloud-core](https://github.com/BlazeInferno64/soundcloud-core) | 6 |
| [MinakowDev/freakcloud](https://github.com/MinakowDev/freakcloud) | 28 |
| [Illogicalll/sctui](https://github.com/Illogicalll/sctui) | 23 |
| [chrisdruta/nimbus](https://github.com/chrisdruta/nimbus) | 47 |
| [yurenip/audionode](https://github.com/yurenip/audionode) | 4 |
| [imthaghost/scdl](https://github.com/imthaghost/scdl) | 4 |
| [enderice2/soundcloud-desktop](https://github.com/enderice2/soundcloud-desktop) | 2 |
| [hellsontime/scdl](https://github.com/hellsontime/scdl) | 4 |
| [dissstructed1337/soundcloud-desktop](https://github.com/dissstructed1337/soundcloud-desktop) | 5 |
| [1vilio/sixonefour](https://github.com/1vilio/sixonefour) | 5 |
| [devilAPI/soundcloud-adfree-uwp-app](https://github.com/devilAPI/soundcloud-adfree-uwp-app) | 1 |
| [sumanthratna/soundcloud9000](https://github.com/sumanthratna/soundcloud9000) | 2 |
| [soroushchehresa/vue-soundcloud](https://github.com/soroushchehresa/vue-soundcloud) | 9 |
| [andreystarkov/overabstracted](https://github.com/andreystarkov/overabstracted) | 5 |
| [barenddt/soundify](https://github.com/barenddt/soundify) | 2 |
| [yunghog/soundcloud-coverart-downloader](https://github.com/yunghog/soundcloud-coverart-downloader) | 2 |
| [keshavbhatt/orange](https://github.com/keshavbhatt/orange) | 2 |
| [giuseppebianchi/loud](https://github.com/giuseppebianchi/loud) | 95 |
| [rwieruch/favesound-redux](https://github.com/rwieruch/favesound-redux) | 8 |
| [rwieruch/favesound-mobx](https://github.com/rwieruch/favesound-mobx) | 8 |
| [rvpanoz/soundload](https://github.com/rvpanoz/soundload) | 3 |
| [bspst/soundground](https://github.com/bspst/soundground) | 3 |
