import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/strings_tr.dart';
import '../../data/auth_repository.dart';
import '../../data/models/app_user.dart';

/// Ana ekran. Aşama 4'te sekmeler, arama ve kayıt listeleriyle doldurulacak.
class HomeScreen extends ConsumerWidget {
  const HomeScreen({super.key, required this.user});

  final AppUser user;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      appBar: AppBar(
        title: const Text(Str.appName),
        actions: [
          IconButton(
            tooltip: Str.signOut,
            icon: const Icon(Icons.logout),
            onPressed: () => ref.read(authRepositoryProvider).signOut(),
          ),
        ],
      ),
      body: Center(
        child: Text(
          user.email ?? user.displayName ?? '',
          style: Theme.of(context).textTheme.titleMedium,
        ),
      ),
    );
  }
}
