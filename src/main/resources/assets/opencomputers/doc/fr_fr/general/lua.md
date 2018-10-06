# Lua

Le [manuel de référence](http://www.lua.org/manual/5.2/manual.html) en Lua et le livre [Programming Lua](http://www.lua.org/pil/) (la première édition est disponible gratuitement en ligne) sont un bon endroit pour démarrer avec les bases de Lua et se familiariser avec la syntaxe de base et les bibliothèques standard. [OpenOS](openOS.md) s'efforce d'émuler les bibliothèques standard au plus près, avec quelques écarts, comme la bibliothèque de débogage qui est principalement absente (pour des raisons de sécurité liées au mode "bac à sable"). Ces différences sont [documentées sur le wiki](https://ocdoc.cil.li/api:non-standard-lua-libs).

Les bibliothèques non-standard devront être importées avec `require` pour les utiliser dans un script. Par exemple :

`local component = require("component")`
`local rs = component.redstone`

Cela vous permettra d'appeller toutes les fonctions fournies par le composant de [redstone](../item/redstoneCard1.md). Par exemple :

`rs.setOutput(require("sides").front, 15)`

**Important**: quand vous travaillez avec l'interpréteur Lua, n'utilisez *pas* `local`, car les variables seront locales à la ligne. Cela signifie que si vous tapiez les lignes ci-dessus les unes après les autres dans l'interpréteur, la troisième ligne lancerait une erreur, en vous disant que `rs` a une valeur `nil`. Mais dans ce cas, pourquoi seulement la troisième ligne ? Parce que, pour faciliter les tests, l'interpréteur essaye de charger les variables inconnues comme des bibliothèques. Donc même si l'affectation à `component` à la première ligne ne ferait rien, l'utilisation de `component` à la deuxième ligne chargerait cette bibliothèque et l'utiliserait. Les bibliothèques ne sont pas utilisées automatiquement lorsque que vous utilisez des scripts Lua, afin de réduire l'usage de la mémoire, car c'est une ressource limitée.

OpenOS fournit plusieurs bibliothèques personnalisées qui peuvent être utilisées dans un grand nombre d'applications, allant du contrôle et la manipulation des composants attachés à l'[ordinateur](computer.md), aux APIs de référence pour les couleurs, utilisées pour le contrôle de câbles de redstone empaquetés, et les codes du [clavier](../block/keyboard.md). Des bibliothèques personnalisées peuvent être employée dans un script Lua en utilisant la fonction `require()`, comme ci-dessus. Certaines bibliothèques personnalisées nécessitent des composants spécifiques pour fonctionner, comme la bibliothèque `internet` qui requiert une [carte internet](../item/internetCard.md). Dans ce cas particulier, la bibliothèque est même fournie par la carte, c'est à dire que la bibliothèque apparaîtra une fois que vous aurez installé une carte internet - techniquement parlant, elle est contenue dans un petit fichier en lecture seule sur la carte internet.
