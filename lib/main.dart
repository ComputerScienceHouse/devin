import 'package:flutter/material.dart';
import 'package:oauth2_client/oauth2_helper.dart';
import 'package:http/http.dart' as http;
import 'csh_oauth.dart';
import 'drink_machine.dart';
import 'dart:convert';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:dynamic_color/dynamic_color.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  static final _defaultLightColorScheme =
      ColorScheme.fromSwatch(primarySwatch: Colors.pink);

  static final _defaultDarkColorScheme = ColorScheme.fromSwatch(
      primarySwatch: Colors.pink, brightness: Brightness.dark);

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return DynamicColorBuilder(builder: (lightColorScheme, darkColorScheme) {
      return MaterialApp(
        title: 'Flask',
        theme: ThemeData(
          colorScheme: lightColorScheme ?? _defaultLightColorScheme,
          useMaterial3: true,
        ),
        darkTheme: ThemeData(
          colorScheme: darkColorScheme ?? _defaultDarkColorScheme,
          useMaterial3: true,
        ),
        // themeMode: ThemeMode.light,
        home: const MyHomePage(title: 'Flask'),
      );
    });
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({Key? key, required this.title}) : super(key: key);

  // This widget is the home page of your application. It is stateful, meaning
  // that it has a State object (defined below) that contains fields that affect
  // how it looks.

  // This class is the configuration for the state. It holds the values (in this
  // case the title) provided by the parent (in this case the App widget) and
  // used by the build method of the State. Fields in a Widget subclass are
  // always marked "final".

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class ThinMachine {
  ThinMachine({
    required this.name,
    required this.display_name,
    required this.icon,
  });

  final String name;
  final String display_name;
  final IconData icon;
}

class _MyHomePageState extends State<MyHomePage> {
  static final transparent =
      const Color(0xFFFFFF); // fully transparent white (invisible)

  int _counter = 0;
  late Future<List<DrinkMachine>> _drinkList;

  late OAuth2Helper _oauth2Helper;

  List<ThinMachine> _thinMachines = [
    ThinMachine(
      name: 'bigdrink',
      display_name: 'Big Drink',
      icon: Icons.sports_bar,
    ),
    ThinMachine(
      name: 'snack',
      display_name: 'Snack',
      icon: Icons.ramen_dining,
    ),
    ThinMachine(
      name: 'littledrink',
      display_name: 'Little Drink',
      icon: Icons.emoji_food_beverage,
    ),
  ];

  int _selectedMachine = 0;
  late Future<int?> _creditCount;

  Future<List<DrinkMachine>> _getDrinkList() async {
    print("Fetching drink list");
    http.Response resp =
        await _oauth2Helper.get("https://drink.csh.rit.edu/drinks");
    final json = jsonDecode(resp.body);
    return json["machines"]
        .map<DrinkMachine>((e) => DrinkMachine.fromJson(e))
        .toList();
  }

  String? _username;

  Future<int?> _getCreditCount() async {
    if (_username == null) {
      final resp = await _oauth2Helper.get(
          "https://sso.csh.rit.edu/auth/realms/csh/protocol/openid-connect/userinfo");
      final json = jsonDecode(resp.body);
      _username = json["preferred_username"]!;
    }
    return _oauth2Helper
        .get("https://drink.csh.rit.edu/users/credits?uid=" + _username!)
        .then((resp) {
      final json = jsonDecode(resp.body);
      return int.parse(json["user"]!["drinkBalance"]!);
    });
  }

  @override
  void initState() {
    super.initState();

    CSHOAuth2Client client = CSHOAuth2Client(
      redirectUri: 'edu.rit.csh.flask://oauth2redirect',
      customUriScheme: 'edu.rit.csh.flask',
    );
    this._oauth2Helper = OAuth2Helper(client,
        grantType: OAuth2Helper.AUTHORIZATION_CODE,
        clientId: 'devin',
        // I'm convinced this is safe to have here, but I'm not sure.
        clientSecret: '3seokwNyQFXnZ7awkZ703xFkS3zihlWY',
        scopes: ['openid', 'profile', 'drink_balance']);
    final tokenFuture = this._oauth2Helper.getToken();
    this._drinkList = tokenFuture.then((_) => this._getDrinkList());
    this._creditCount = tokenFuture.then((_) => this._getCreditCount());
  }

  void _onSelectMachine(int index) {
    this.setState(() {
      this._selectedMachine = index;
    });
  }

  int getMachineIndex() {
    return this._selectedMachine - 1;
  }

  Future<void>? _dropping = null;
  bool _usdUnit = false;

  Future<void> _dropDrink(String machineName, int slotNumber) async {
    if (this._dropping != null) {
      throw new Exception("Already dropping!");
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
    this.setState(() {
      if (json["drinkBalance"] != null) {
        this._creditCount = Future.value(json["drinkBalance"]);
      } else {
        this._creditCount = this._getCreditCount();
      }
      this._drinkList = this._getDrinkList();
    });
    print("Finished with dropping a drink! " + resp.body);
  }

  Widget _buildSlot(
      BuildContext context, ThinMachine machine, MachineSlot slot) {
    return Card(
      child: InkWell(
        onTap: null,
        child: ListTile(
            title: Row(children: [
              Text(slot.item.name),
              Expanded(
                  child: Align(
                      alignment: Alignment.centerRight,
                      child: InkWell(
                          onTap: () {
                            this.setState(() {
                              this._usdUnit = !this._usdUnit;
                            });
                          },
                          child: Chip(
                            avatar: Icon(Icons.attach_money,
                                semanticLabel: "Price"),
                            label: Text(this._usdUnit
                                ? ("\$" +
                                    (slot.item.price / 100).toStringAsFixed(2))
                                : (slot.item.price.toString() + " Credits")),
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
                                primary: Theme.of(context).colorScheme.primary,
                                onPrimary:
                                    Theme.of(context).colorScheme.onPrimary,
                              ),
                              onPressed: (snapshot.data == null ||
                                      snapshot.data! >= slot.item.price)
                                  ? () {
                                      this.setState(() {
                                        this._dropping = this._dropDrink(
                                            machine.name, slot.number);
                                        this._dropping!.then((_) {
                                          this.setState(() {
                                            this._dropping = null;
                                          });
                                        });
                                      });
                                    }
                                  : null,
                              child: const Text('Buy Now'),
                            ))),
                  ]);
                }),
            leading: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                Padding(
                    padding: EdgeInsets.only(top: 12),
                    child: Icon(machine.icon, semanticLabel: machine.name))
              ],
            )),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
      ),
      body: Center(
        child: FutureBuilder<List<DrinkMachine>>(
          future: _drinkList,
          builder: (context, snapshot) {
            print("State of snapshot: " + snapshot.connectionState.toString());
            if (snapshot.hasData) {
              List<MachineSlot> slots;
              if (this.getMachineIndex() >= 0) {
                final thinMachine = this._thinMachines[this.getMachineIndex()];
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
                machineMap[snapshot.data![i].id] = this
                    ._thinMachines
                    .firstWhere((e) => e.name == snapshot.data![i].name);
              }
              return RefreshIndicator(
                child: ListView.builder(
                  padding: const EdgeInsets.all(8),
                  itemCount: slots.length,
                  itemBuilder: (context, index) => this._buildSlot(
                      context, machineMap[slots[index].machine]!, slots[index]),
                ),
                onRefresh: () async {
                  final drinkList = this._getDrinkList();
                  final creditCount = this._getCreditCount();
                  this.setState(() {
                    this._drinkList = drinkList;
                    this._creditCount = creditCount;
                  });
                  return Future.wait([drinkList, creditCount]).then((_) => {});
                },
              );
            } else if (snapshot.hasError) {
              return Text("${snapshot.error}");
            }
            return CircularProgressIndicator();
          },
        ),
      ),
      // floatingActionButton: FloatingActionButton(
      //   onPressed: _incrementCounter,
      //   tooltip: 'Increment',
      //   child: const Icon(Icons.add),
      // ),

      bottomNavigationBar: BottomNavigationBar(
        type: BottomNavigationBarType.fixed,
        items: [
              BottomNavigationBarItem(
                icon: Icon(Icons.star),
                label: 'All',
              )
            ] +
            _thinMachines
                .map((e) => BottomNavigationBarItem(
                      icon: Icon(e.icon),
                      label: e.display_name,
                    ))
                .toList(),
        currentIndex: _selectedMachine,
        // selectedItemColor: Colors.amber[800],
        onTap: _onSelectMachine,
      ),
    );
  }
}
