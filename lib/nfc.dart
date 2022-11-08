import 'package:oauth2_client/oauth2_helper.dart';
import 'dart:convert';
import 'package:flutter/services.dart';
import 'package:http/http.dart' as http;

class AIDPack {
  const AIDPack({
    required this.doorsId,
    required this.drinkId,
    required this.memberProjectsId,
  });
  final String doorsId;
  final String drinkId;
  final String memberProjectsId;

  Map<String, String> toMap() {
    return {
      "doors": doorsId,
      "drink": drinkId,
      "memberProjects": memberProjectsId,
    };
  }
}

class Nfc {
  const Nfc({
    required this.oauth2Helper,
  });
  final OAuth2Helper oauth2Helper;
  static const nfc = MethodChannel("edu.rit.csh.devin/nfc");

  static const gkBase = "https://gatekeeper-v2.csh.rit.edu";

  Future<void> syncAid() async {
    late http.Response response;
    try {
      response = await oauth2Helper.get("$gkBase/mobile/provision");
    } on MissingPluginException catch (_) {
      print("Warning: NFC plugin not found");
      return;
    }
    if (response.statusCode == 200) {
      final json = jsonDecode(response.body);
      final aid = AIDPack(
        doorsId: json["doorsId"],
        drinkId: json["drinkId"],
        memberProjectsId: json["memberProjectsId"],
      ).toMap();
      await nfc.invokeMethod("updateAid", aid);
    } else {
      throw Exception("Failed to sync AID");
    }
  }
}
