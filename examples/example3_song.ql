import "json:json.ql"

string jsonStr = "{\"a\": \"Hello\"}"

JsonConverter json = JsonConverter.createJsonConverter(true)

Map map = json.fromJson(jsonStr)
print("a = " + map.get("a"))

// Modify the map

song someSong = "Millstone" by "ROSARY"
map.put("mysong", someSong)

string newJsonStr = json.toJson(map)
print(newJsonStr)

// Reserialize it
Map updatedMap = json.fromJson(newJsonStr)
print("Song is " + updatedMap.get("mysong").getName() + " and is called " + updatedMap.get("mysong").getArtist())

