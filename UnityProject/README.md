# Road Runner 3D — Unity 2022.3 LTS

A real 3D endless-runner Android project, kept separate from the previous Kotlin/Canvas prototype.

## Generate the complete scene

1. Open the `UnityProject` folder in Unity Hub using Unity **2022.3 LTS** with Android Build Support (SDK, NDK and OpenJDK) installed.
2. Wait for Unity to import scripts. In the editor choose **Road Runner 3D → Build Complete 3D Scene**.
3. The editor tool creates `Assets/Scenes/Runner.unity`, 3D materials, the obstacle car prefab, road, lane markings, roadside trees, perspective camera, lighting, player car, spawner and game manager.
4. In **Edit → Project Settings → Player → Other Settings**, set **Active Input Handling** to **Input Manager (Old)** or **Both**.
5. In **File → Build Settings**, choose Android, switch platform, confirm Runner scene is enabled, then Build.

## Controls and gameplay

- Three lanes centered at X = -2, 0, +2.
- Swipe horizontally at least 50 pixels to change lanes. In Editor use A/D or arrow keys.
- Player root starts at (0, 0.5, 0); road is a 3D Cube at (0,0,100), scale (6,0.2,200).
- Two lane markers at X = -1 and +1.
- Obstacles spawn in exactly one lane at Z=60 and Y=0.5, travel toward negative Z and are removed below Z=-20. Two lanes remain open.
- Score, coins, best score and car ownership persist using PlayerPrefs. Includes menu, shop and replay after collision.

## Scope and honest status

The gameplay world uses Unity 3D primitives (Cube, Cylinder and Sphere), MeshRenderers and 3D physics colliders. There are no SpriteRenderers, 2D camera or runtime mesh-generation calls. The scene is generated inside the Unity Editor by the included Editor script.

This repository contains the Unity source and scene generator, **not a compiled APK**. It has not yet been imported or compiled by Unity in this environment. A Unity 2022.3 Editor installation and valid Unity license are required to produce the Android APK.
