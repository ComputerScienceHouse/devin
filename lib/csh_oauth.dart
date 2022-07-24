import 'package:oauth2_client/oauth2_client.dart';

class CSHOAuth2Client extends OAuth2Client {
  CSHOAuth2Client(
      {required String redirectUri, required String customUriScheme})
      : super(
            authorizeUrl:
                'https://sso.csh.rit.edu/auth/realms/csh/protocol/openid-connect/auth',
            tokenUrl:
                'https://sso.csh.rit.edu/auth/realms/csh/protocol/openid-connect/token', //Your service access token url
            redirectUri: redirectUri,
            customUriScheme: customUriScheme);
}
