Problem:

Firebase requires recent Android version.

[This old piece][1] solved the problem. Warning: deprecated API.

Firebase doesn’t support JSONArray, but supports plain List/Map. Try a converter.

[1]:	https://github.com/mimming/firebase-fruit-detector/blob/master/google-glass/app/src/main/java/com/firebase/sample/fruitdetector/DetectFruitActivity.java