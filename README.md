# ♕ BYU CS 240 Chess

This project demonstrates mastery of proper software design, client/server architecture, networking using HTTP and WebSocket, database persistence, unit testing, serialization, and security.

## 10k Architecture Overview

The application implements a multiplayer chess server and a command line chess client.

[![Sequence Diagram](10k-architecture.png)](https://sequencediagram.org/index.html#initialData=C4S2BsFMAIGEAtIGckCh0AcCGAnUBjEbAO2DnBElIEZVs8RCSzYKrgAmO3AorU6AGVIOAG4jUAEyzAsAIyxIYAERnzFkdKgrFIuaKlaUa0ALQA+ISPE4AXNABWAexDFoAcywBbTcLEizS1VZBSVbbVc9HGgnADNYiN19QzZSDkCrfztHFzdPH1Q-Gwzg9TDEqJj4iuSjdmoMopF7LywAaxgvJ3FC6wCLaFLQyHCdSriEseSm6NMBurT7AFcMaWAYOSdcSRTjTka+7NaO6C6emZK1YdHI-Qma6N6ss3nU4Gpl1ZkNrZwdhfeByy9hwyBA7mIT2KAyGGhuSWi9wuc0sAI49nyMG6ElQQA)

Web API Sequence Diagram: (https://sequencediagram.org/index.html?presentationMode=readOnly#initialData=IYYwLg9gTgBAwgGwJYFMB2YYCgAOwphIhJ4bzLpi76HGmYDKKUAbs9QUScGQBI8ATBOzyc6PRsxZEUHWtzIARYGGABBECBQBnbVgErgAI2DaUMAUaxZKUAJ7a8xNAHMYABgB0ATmsuoEACuODAAxGjALHYwAEooLkjaYFAqSBBoYQDuABZIYLKIqGQAtAB8MEyszABcMADaAAoA8gwAKgC6MAD0gWZQADpoAN4ARL3MEQC2KCPVIzAjADQLeLqZ0AKz80sLKJPASAhbCwC+WJVssGUw-GhCNTCj41BTM3MLyyOr2utQm+87EZ7A5HAFnW73K7lC4yWpQeKJfJQAAUcQSSWYcQAjoEdGAAJTnKQyGDXZSqDRaXS1FwoMAAVT6yOer0J5PUmh02lJ5UstQAYkg7jBGcx2TAjNEWcBplh2ZSuaTrjCtLU0IEEAgiawSWTDArqTAQPCVChRSjnuy2frOboeRYjLU1AIBCK+uztdItPb5bbtLVjShTWpAmBssjgKHsla5TaqdzrnyYM7XSGwx6VeZitcIcIoLU0YjMToNVRc8wldCpA8nn1XvNattPpGw60IABrdANhanT0V7PlQqUWoAJnc7kGtYmMpmMEbHwWLeybc7aG72zO6AEfgCwTC0B4tJgABkIAkMqEcnkChQSlWqvn6s02p0umZdGk0JOxnWZ8dAd8vz-BufZQjcgh5rUU4vH+AKfIBGz-uCEH9veXooLUCBnkKyKnue2K4kkhKZj6cZcjSdLmsyv7TNaFJ+vaSZxMkqBsG6YqGLG9HxpWFTEqq7FQOKABmQR3J6urlL68YBia+RpuGS4xtJiqJo6yYusmUYZvxWY5ihj54UKcTaKWWDllcyrVo+0H1nOPaAkuK5dvZIEXP21xDhgo7jt+0rTOuC4jE5HYufOIybuJWD+EEIShP4KDoCeZ5BJgl65PkWBeZgVkPrUdSKAAosehWtIVr7vton6DCFq6gfaFm1LV6DmQZ9qZphKWhhGUbOWgxG6aR3HkRYKDCPJUY9a2oX9VxHI8WpBZ7BAbEKTpOregO4F3JByUuKlMCmX6rU7R5aE1mc7mWYOt5gD5E7DJF27RbucXwq6x6IjAADiM7cul15Zbd7XWfl32la+LgzjVvUzfV+mnY+zVoCdkIgxtGEwMgSS-dM2hTcuM0DRjQ3zSNtJgApBN9XRZN2otMAAGrAMgBj5DAyNzQaCa5ehTraYYMCXEgwmoNuJF6sNhrwixKBsGomq4zoyK09zjHqfydIgNkMBQ9MWOIlzDFbR1J5fRAwm639EmbQjkK1Ern1JCZZkWbxV1QSMes6LM9SeP77SXdZvHZfd37e9ovt1P7niBzYUUxXuoQ4IEUA4MI8ByeYStZBlN5FDl52Po0LQdN03sw9Nq7hzOABysFucHdu7aMyP-p83v1wFYKo3m6N80aWdK8icBDzOBF4sT6Gk9zFGU5NyOq8bvLqczrOmhzsN1Sp9O87CWnpoLwuiyg4uDZLdP+jAQp9GAw9K8pZH0yvtQMJA8IwGgKCZFbso7zzaF95KwAJKKF1ugZgpoz4ky2o1TOQZ8hKxdggMsbUTag0eF7GcoDfYAEYRwAGYAAsQcHwh1umHYYWDpg4NqPg4hT0dyxTCHYMaWEf4ACkIBCh+jOMIRgECgHbEDAu-cHiNHpGXLoFcIhb3QN+dOwBWFQDgBALCUAtgAHVeDALKl0AAQseNQcAADSgJva0JgPQkh8NyhwNbnItccEViCOUao9R7cFgWMUHgwhNi3Z7wEgAK24WgYeXChRIJQDiSeNssxSSflfCmVNF5GwWi-JmLMkBs3MJzf+7tdL80PqoIWzARZiziTPP0c9h7eKXukh0S1ZZsW9mk1SgTMY51EoEcSEsElSyvtrFAIB2wNFccwNQLAQTGEOHkOwyJFFuLUdAep7TAECQYDgCAmBIjTIEbIPpMB-61GCDkpWmi8jZDGUo5gyJvHLEWcwdxKy2nP0aSKHAOTf7mDZsASpsCDK1AiWE8eJYUG9zOnxPKjxSGXHIQXShQxGEvWYfFFI0REBBlgMAHA6ciCpHSLnQG2UxHFyKiVMqr5LC2O2vbcgWKYDwhiUkCFYFTYgGEPgdkJgzAq3+f0y+tQBBjTpCgBWCBzT41WW8pMmL8CCXWtPC+s9RrjTFYrP6fL8kMzlbAB+nFDnHNVaK8VCk+pSteQmDJuqD7RgNefZuDwbVHXjKygp0KkXw08hQmAY4Hqeq3NYIAA)

## Modules

The application has three modules.

- **Client**: The command line program used to play a game of chess over the network.
- **Server**: The command line program that listens for network requests from the client and manages users and games.
- **Shared**: Code that is used by both the client and the server. This includes the rules of chess and tracking the state of a game.

## Starter Code

As you create your chess application you will move through specific phases of development. This starts with implementing the moves of chess and finishes with sending game moves over the network between your client and server. You will start each phase by copying course provided [starter-code](starter-code/) for that phase into the source code of the project. Do not copy a phases' starter code before you are ready to begin work on that phase.

## IntelliJ Support

Open the project directory in IntelliJ in order to develop, run, and debug your code using an IDE.

## Maven Support

You can use the following commands to build, test, package, and run your code.

| Command                    | Description                                     |
| -------------------------- | ----------------------------------------------- |
| `mvn compile`              | Builds the code                                 |
| `mvn package`              | Run the tests and build an Uber jar file        |
| `mvn package -DskipTests`  | Build an Uber jar file                          |
| `mvn install`              | Installs the packages into the local repository |
| `mvn test`                 | Run all the tests                               |
| `mvn -pl shared test`      | Run all the shared tests                        |
| `mvn -pl client exec:java` | Build and run the client `Main`                 |
| `mvn -pl server exec:java` | Build and run the server `Main`                 |

These commands are configured by the `pom.xml` (Project Object Model) files. There is a POM file in the root of the project, and one in each of the modules. The root POM defines any global dependencies and references the module POM files.

## Running the program using Java

Once you have compiled your project into an uber jar, you can execute it with the following command.

```sh
java -jar client/target/client-jar-with-dependencies.jar

♕ 240 Chess Client: chess.ChessPiece@7852e922
```
