import 'package:flutter/material.dart';
import 'package:oauth2_client/oauth2_helper.dart';
import 'package:http/http.dart' as http;
import 'csh_oauth.dart';
import 'drink_machine.dart';
import 'dart:convert';
import 'package:flutter/foundation.dart' show kIsWeb;

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flask',
      theme: ThemeData(
        // This is the theme of your application.
        //
        // Try running your application with "flutter run". You'll see the
        // application has a blue toolbar. Then, without quitting the app, try
        // changing the primarySwatch below to Colors.green and then invoke
        // "hot reload" (press "r" in the console where you ran "flutter run",
        // or simply save your changes to "hot reload" in a Flutter IDE).
        // Notice that the counter didn't reset back to zero; the application
        // is not restarted.
        primarySwatch: Colors.pink,
        useMaterial3: true,
      ),
      home: const MyHomePage(title: 'Flask'),
    );
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

  void _incrementCounter() {
    setState(() {
      // This call to setState tells the Flutter framework that something has
      // changed in this State, which causes it to rerun the build method below
      // so that the display can reflect the updated values. If we changed
      // _counter without calling setState(), then the build method would not be
      // called again, and so nothing would appear to happen.
      _counter++;
      this._drinkList = this._getDrinkList();
    });
  }

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
    // return Future.value(null);
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

  @override
  Widget build(BuildContext context) {
    // This method is rerun every time setState is called, for instance as done
    // by the _incrementCounter method above.
    //
    // The Flutter framework has been optimized to make rerunning build methods
    // fast, so that you can just rebuild anything that needs updating rather
    // than having to individually change instances of widgets.

    return Scaffold(
      appBar: AppBar(
        // Here we take the value from the MyHomePage object that was created by
        // the App.build method, and use it to set our appbar title.
        title: Text(widget.title),
      ),
      body: Center(
        // Center is a layout widget. It takes a single child and positions it
        // in the middle of the parent.
        child: FutureBuilder<List<DrinkMachine>>(
          future: _drinkList,
          builder: (context, snapshot) {
            print("State of snapshot: " + snapshot.connectionState.toString());
            if (snapshot.hasData) {
              List<MachineSlot> slots;
              if (this.getMachineIndex() >= 0) {
                final thinMachine = this._thinMachines[this.getMachineIndex()];
                slots = snapshot.data!.firstWhere((machine) => machine.name == thinMachine.name).slots;
              } else {
                slots = [];
                for (int i = 0; i < snapshot.data!.length; i++) {
                  slots.addAll(snapshot.data![i].slots);
                }
              }
              slots.retainWhere((e) =>
                  e.active && (e.count == null || e.count! > 0) && !e.empty);
              Map<int, ThinMachine> machineMap = {};
              for (int i = 0; i < snapshot.data!.length; ++i) {
                machineMap[snapshot.data![i].id] = this
                    ._thinMachines
                    .firstWhere((e) => e.name == snapshot.data![i].name);
              }
              return ListView.builder(
                padding: const EdgeInsets.all(8),
                itemCount: slots.length,
                itemBuilder: (context, index) {
                  // print("Item builder?");
                  // This is slow, but I don't really care
                  ThinMachine machine = machineMap[slots[index].machine]!;
                  return Card(
                    child: InkWell(
                        onTap: null,
                        child: ListTile(
                          title: Row(children: [
                            Text(slots[index].item.name),
                            Expanded(
                                child: Align(
                                    alignment: Alignment.centerRight,
                                    child: Chip(
                                      avatar: Icon(Icons.payments,
                                          semanticLabel: "Price"),
                                      label: Text(
                                          slots[index].item.price.toString()),
                                    )))
                          ]),
                          subtitle: Row(children: [
                            Align(
                              alignment: Alignment.centerRight,
                              child: FutureBuilder<int?>(
                                  future: _creditCount,
                                  builder: (context, snapshot) {
                                    return ElevatedButton(
                                      onPressed: (snapshot.data == null ||
                                              snapshot.data! >=
                                                  slots[index].item.price)
                                          ? () {
                                              this.setState(() {
                                                this._dropping = this
                                                    ._dropDrink(
                                                        machineMap[slots[index]
                                                                .machine]!
                                                            .name,
                                                        slots[index].number);
                                                this._dropping!.then((_) {
                                                  this.setState(() {
                                                    this._dropping = null;
                                                  });
                                                });
                                              });
                                              print(slots[index]
                                                  .number
                                                  .toString());
                                            }
                                          : null,
                                      child: const Text('Buy'),
                                    );
                                  }),
                            )
                          ]),
                          leading:
                              Icon(machine.icon, semanticLabel: machine.name),
                        )),
                  );
                },
              );
            } else if (snapshot.hasError) {
              return Text("${snapshot.error}");
            }
            return CircularProgressIndicator();
          },
        ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _incrementCounter,
        tooltip: 'Increment',
        child: const Icon(Icons.add),
      ), // This trailing comma makes auto-formatting nicer for build methods.

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
