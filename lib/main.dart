import 'package:flutter/material.dart';
import 'package:oauth2_client/oauth2_helper.dart';
import 'package:http/http.dart' as http;
import 'csh_oauth.dart';
import 'drink_machine.dart';
import 'nfc.dart';
import 'dart:convert';
import 'package:dynamic_color/dynamic_color.dart';
import 'package:wear_bridge/wear_bridge.dart';
import 'dart:core';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  static final _defaultLightColorScheme =
      ColorScheme.fromSwatch(primarySwatch: Colors.pink);

  static final _defaultDarkColorScheme = ColorScheme.fromSwatch(
      primarySwatch: Colors.pink, brightness: Brightness.dark);

  static final _watchColorScheme = ColorScheme.fromSwatch(
      primarySwatch: Colors.pink,
      brightness: Brightness.dark,
      backgroundColor: Colors.black);

  static final _isWatch = WearBridge.isWatch();

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return FutureBuilder<bool?>(
        future: _isWatch,
        builder: (context, snapshot) {
          final isWatch = snapshot.data ?? false;
          return DynamicColorBuilder(
              builder: (lightColorScheme, darkColorScheme) {
            final darkTheme = ThemeData.from(
              colorScheme: isWatch
                  ? _watchColorScheme
                  : (darkColorScheme ?? _defaultDarkColorScheme),
              useMaterial3: true,
            );
            return MaterialApp(
              title: 'Flask',
              theme: ThemeData.from(
                colorScheme: lightColorScheme ?? _defaultLightColorScheme,
                useMaterial3: true,
              ),
              darkTheme: darkTheme,
              themeMode: isWatch ? ThemeMode.dark : ThemeMode.system,
              home: MyHomePage(title: 'Flask', isWatch: isWatch),
            );
          });
        });
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({Key? key, required this.title, required this.isWatch})
      : super(key: key);

  // This widget is the home page of your application. It is stateful, meaning
  // that it has a State object (defined below) that contains fields that affect
  // how it looks.

  // This class is the configuration for the state. It holds the values (in this
  // case the title) provided by the parent (in this case the App widget) and
  // used by the build method of the State. Fields in a Widget subclass are
  // always marked "final".

  final String title;
  final bool isWatch;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class ThinMachine {
  const ThinMachine({
    required this.name,
    required this.displayName,
    required this.icon,
  });

  final String name;
  final String displayName;
  final IconData icon;
}

class _MyHomePageState extends State<MyHomePage> {
  late Future<List<DrinkMachine>> _drinkList;

  late final OAuth2Helper _oauth2Helper;
  late final Nfc _nfc;

  static final _thinMachines = [
    const ThinMachine(
      name: 'bigdrink',
      displayName: 'Big Drink',
      icon: Icons.sports_bar,
    ),
    const ThinMachine(
      name: 'snack',
      displayName: 'Snack',
      icon: Icons.ramen_dining,
    ),
    const ThinMachine(
      name: 'littledrink',
      displayName: 'Little Drink',
      icon: Icons.emoji_food_beverage,
    ),
  ];

  int _selectedMachine = 0;
  late Future<int?> _creditCount;

  Future<List<DrinkMachine>> _getDrinkList() async {
    http.Response resp =
        await _oauth2Helper.get("https://drink.csh.rit.edu/drinks");
    final json = jsonDecode(resp.body);
    return json["machines"]
        .map<DrinkMachine>((e) => DrinkMachine.fromJson(e))
        .toList();
  }

  late Future<String> _username;

  Future<String> _getUsername() async {
    final resp = await _oauth2Helper.get(
        "https://sso.csh.rit.edu/auth/realms/csh/protocol/openid-connect/userinfo");
    final json = jsonDecode(resp.body);
    return json["preferred_username"]!;
  }

  Future<int?> _getCreditCount() async {
    return _username
        .then((username) => _oauth2Helper.get(
            "https://drink.csh.rit.edu/users/credits?uid=${Uri.encodeQueryComponent(username)}"))
        .then((resp) {
      final json = jsonDecode(resp.body);
      return int.parse(json["user"]!["drinkBalance"]!);
    });
  }

  @override
  void initState() {
    super.initState();

    CSHOAuth2Client client = CSHOAuth2Client(
      redirectUri: 'edu.rit.csh.devin://oauth2redirect',
      customUriScheme: 'edu.rit.csh.devin',
    );
    _oauth2Helper = OAuth2Helper(client,
        grantType: OAuth2Helper.authorizationCode,
        clientId: 'devin',
        // I'm convinced this is safe to have here, but I'm not sure.
        clientSecret: '3seokwNyQFXnZ7awkZ703xFkS3zihlWY',
        scopes: ['openid', 'email', 'groups', 'profile', 'drink_balance']);
    final tokenFuture = _oauth2Helper.getToken();
    _username = tokenFuture.then((_) => _getUsername());
    _drinkList = tokenFuture.then((_) => _getDrinkList());
    _creditCount = tokenFuture.then((_) => _getCreditCount());
    _nfc = Nfc(oauth2Helper: _oauth2Helper);
    tokenFuture.then((_) => _nfc.syncAid());
  }

  void _onSelectMachine(int index) {
    setState(() {
      _selectedMachine = index;
    });
  }

  int getMachineIndex() {
    return _selectedMachine - 1;
  }

  Future<void>? _dropping;
  bool _usdUnit = false;

  Future<void> _dropDrink(String machineName, int slotNumber) async {
    if (_dropping != null) {
      throw Exception("Already dropping!");
    }
    final resp = await _oauth2Helper.post(
      "https://drink.csh.rit.edu/drinks/drop",
      headers: {
        "Content-Type": "application/json",
      },
      body: jsonEncode({
        "machine": machineName,
        "slot": slotNumber,
      }),
    );
    final json = jsonDecode(resp.body);
    setState(() {
      if (json["drinkBalance"] != null) {
        _creditCount = Future.value(json["drinkBalance"]);
      } else {
        _creditCount = _getCreditCount();
      }
      _drinkList = _getDrinkList();
    });
    // print("Finished with dropping a drink! ${resp.body}");
  }

  Widget _buildSlot(
      BuildContext context, ThinMachine machine, MachineSlot slot) {
    final icon = Icon(machine.icon, semanticLabel: machine.name);

    void onDrop() {
      setState(() {
        _dropping = _dropDrink(machine.name, slot.number);
        final bar = ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text("Dropping drink..."),
            duration: Duration(days: 4242),
          ),
        );
        _dropping!.then((_) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text("Drink dropped! Enjoy!"),
            ),
          );
        }).catchError((err) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text("Oops! Something went wrong: $err"),
            ),
          );
        }).whenComplete(() {
          bar.close();
          setState(() {
            _dropping = null;
          });
        });
      });
    }

    if (widget.isWatch) {
      return FutureBuilder<int?>(
          future: _creditCount,
          builder: (context, snapshot) {
            return InkWell(
                customBorder: const StadiumBorder(),
                onTap: ((snapshot.data == null ||
                            snapshot.data! >= slot.item.price) &&
                        _dropping == null)
                    ? onDrop
                    : null,
                child: Chip(
                  avatar: icon,
                  label: Text(slot.item.name),
                ));
          });
    }

    return Card(
      child: ListTile(
        title: Row(children: [
          Text(slot.item.name),
          Expanded(
              child: Align(
                  alignment: Alignment.centerRight,
                  child: InkWell(
                      customBorder: const StadiumBorder(),
                      onTap: () {
                        setState(() {
                          _usdUnit = !_usdUnit;
                        });
                      },
                      child: Chip(
                        avatar: const Icon(Icons.attach_money,
                            semanticLabel: "Price"),
                        label: Text(_usdUnit
                            ? ("\$${(slot.item.price / 100).toStringAsFixed(2)}")
                            : ("${slot.item.price.toString()} Credits")),
                      ))))
        ]),
        subtitle: FutureBuilder<int?>(
            future: _creditCount,
            builder: (context, snapshot) {
              return Row(children: [
                Expanded(
                    child: Align(
                        alignment: Alignment.centerLeft,
                        child: ElevatedButton(
                          style: ElevatedButton.styleFrom(
                            backgroundColor:
                                Theme.of(context).colorScheme.primary,
                            foregroundColor:
                                Theme.of(context).colorScheme.onPrimary,
                          ),
                          onPressed: ((snapshot.data == null ||
                                      snapshot.data! >= slot.item.price) &&
                                  _dropping == null)
                              ? onDrop
                              : null,
                          child: const Text('Buy Now'),
                        ))),
              ]);
            }),
        leading: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Padding(padding: const EdgeInsets.only(top: 12), child: icon)
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: widget.isWatch
          ? null
          : AppBar(
              title: Text(widget.title),
              actions: [
                FutureBuilder<int?>(
                  future: _creditCount,
                  builder: (context, snapshot) {
                    if (snapshot.data == null) {
                      return const SizedBox.shrink();
                    }
                    return Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Padding(
                          padding: const EdgeInsets.only(right: 8),
                          child: Text(
                              _usdUnit
                                  ? ("\$${(snapshot.data! / 100).toStringAsFixed(2)}")
                                  : ("${snapshot.data!.toString()} Credits"),
                              style: Theme.of(context).textTheme.titleMedium),
                        )
                      ],
                    );
                  },
                ),
              ],
            ),
      body: Center(
        child: FutureBuilder<List<DrinkMachine>>(
          future: _drinkList,
          builder: (context, snapshot) {
            // print("State of snapshot: ${snapshot.connectionState.toString()}");
            if (snapshot.hasData) {
              List<MachineSlot> slots;
              if (getMachineIndex() >= 0) {
                final thinMachine = _thinMachines[getMachineIndex()];
                slots = snapshot.data!
                    .firstWhere((machine) => machine.name == thinMachine.name)
                    .slots;
              } else {
                slots = [];
                for (int i = 0; i < snapshot.data!.length; i++) {
                  slots.addAll(snapshot.data![i].slots);
                }
              }
              slots.retainWhere((e) =>
                  e.active && (e.count == null || e.count! > 0) && !e.empty);
              slots.sort((a, b) => a.machine == b.machine
                  ? a.number - b.number
                  : a.machine - b.machine);
              Map<int, ThinMachine> machineMap = {};
              for (int i = 0; i < snapshot.data!.length; ++i) {
                machineMap[snapshot.data![i].id] = _thinMachines
                    .firstWhere((e) => e.name == snapshot.data![i].name);
              }
              return RefreshIndicator(
                child: ListView.builder(
                  padding: widget.isWatch
                      ? const EdgeInsets.symmetric(horizontal: 8, vertical: 64)
                      : const EdgeInsets.all(8),
                  itemCount: slots.length,
                  itemBuilder: (context, index) => _buildSlot(
                      context, machineMap[slots[index].machine]!, slots[index]),
                ),
                onRefresh: () async {
                  final drinkList = _getDrinkList();
                  final creditCount = _getCreditCount();
                  setState(() {
                    _drinkList = drinkList;
                    _creditCount = creditCount;
                  });
                  return Future.wait([drinkList, creditCount]).then((_) => {});
                },
              );
            } else if (snapshot.hasError) {
              return Text("${snapshot.error}");
            }
            return const CircularProgressIndicator();
          },
        ),
      ),
      bottomNavigationBar: widget.isWatch
          ? null
          : BottomNavigationBar(
              type: BottomNavigationBarType.fixed,
              items: [
                    const BottomNavigationBarItem(
                      icon: Icon(Icons.star),
                      label: 'All',
                    )
                  ] +
                  _thinMachines
                      .map((e) => BottomNavigationBarItem(
                            icon: Icon(e.icon),
                            label: e.displayName,
                          ))
                      .toList(),
              currentIndex: _selectedMachine,
              // selectedItemColor: Colors.amber[800],
              onTap: _onSelectMachine,
            ),
    );
  }
}
