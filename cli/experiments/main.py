# TODO get from service dependencies
import os
import shutil
import subprocess
from pathlib import Path


def main():
    capture_std = False

    client_to_server = {"org.example.caller.ResponderClient": "org.example.responder.StringController"}

    # TODO get from file scanner
    trap_file = "D:/codeql-dbs/caller/trap/java/E_/java/multi-micro/microservices/caller/src/main/java/org/example/caller/CallController.java.trap"

    # TODO get from GUI or command-line
    db_path = f"D:/codeql-dbs/caller"
    result_file = f"E:/java/multi-micro/microservices/caller/codeql_result.sarif"

    if os.path.exists(db_path):
        shutil.rmtree(db_path)

    result = subprocess.run(
        f'codeql database init -s="E:/java/multi-micro/microservices/caller" -l=java --overwrite -- "D:/codeql-dbs/caller"',
        capture_output=capture_std, text=True)

    if result.stderr:
        # print("Errors:")
        print(result.stderr)

    env = os.environ.copy()
    env["CODEQL_EXTRACTOR_JAVA_OPTION_TRAP_COMPRESSION"] = "NONE"

    # run target build
    result = subprocess.run(
        f'codeql database trace-command --working-dir=E:/java/multi-micro/microservices/caller --index-traceless-dbs --no-db-cluster -- D:/codeql-dbs/caller E:/codeql-bundle-win64/codeql/java/tools/autobuild.cmd',
        capture_output=capture_std, text=True, env=env)

    if result.stderr:
        # print("Errors:")
        print(result.stderr)

    # run dependency build
    # result = subprocess.run(
    #     f'codeql database trace-command --working-dir=E:/java/multi-micro/microservices/responder --index-traceless-dbs --no-db-cluster -- D:/codeql-dbs/caller E:/codeql-bundle-win64/codeql/java/tools/autobuild.cmd',
    #     capture_output=capture_std, text=True, env=env)
    #
    # if result.stderr:
    #     #print("Errors:")
    #     print(result.stderr)

    # modify trap files
    with open(trap_file, 'r', encoding='utf-8') as file:
        file_contents = file.read()

    for f, t in client_to_server.items():
        file_contents = file_contents.replace(f, t)

    with open(trap_file, 'w', encoding='utf-8') as file:
        file.write(file_contents)

    # copy dependency traps
    src = Path(r"D:\codeql-dbs\responder\trap\java\E_\java\multi-micro\microservices\responder")
    dest = Path(r"D:\codeql-dbs\caller\trap\java\E_\java\multi-micro\microservices\responder")
    shutil.copytree(src, dest)

    src = Path(r"D:\codeql-dbs\responder\trap\java\classes\org\example\responder")
    dest = Path(r"D:\codeql-dbs\caller\trap\java\classes\org\example\responder")
    shutil.copytree(src, dest)

    # finalize db
    result = subprocess.run(
        f'codeql database finalize --no-cleanup --no-db-cluster -v -- D:/codeql-dbs/caller',
        capture_output=capture_std, text=True)

    if result.stderr:
        # print("Errors:")
        print(result.stderr)

    # remove old results file
    if os.path.isfile(result_file):
        os.remove(result_file)

    # run query pack
    result = subprocess.run(
        f'codeql database analyze D:/codeql-dbs/caller --format=sarif-latest --output="E:/java/multi-micro/microservices/caller/codeql_result.sarif" --rerun --threads=0 java-lgtm-full --no-sarif-minify --sarif-add-snippets',
        capture_output=capture_std, text=True)

    if result.stderr:
        # print("Errors:")
        print(result.stderr)

    # find client classes
    # client_tags = {}
    # required_server_classes = set()
    # server_tags = {}
    # with open(trap_file, "r", encoding="utf-8") as file:
    #     print('Looking for client classes')
    #     for _, line in enumerate(file, start=1):
    #         for cc in client_to_server:
    #             if 'class;' + cc in line:
    #                 tag = line[1:line.find('=')]
    #                 client_tags[tag] = cc
    #                 required_server_classes.add(client_to_server[cc])
    #
    #                 print(f"Found client class {cc} in tag #{tag}")
    #
    #     print(f"Found {len(client_tags)} client class references")
    #
    #     print('Looking for server classes')
    #     file.seek(0)
    #     for _, line in enumerate(file, start=1):
    #         for sc in required_server_classes:
    #             if 'class;' + sc in line:
    #                 tag = line[1:line.find('=')]
    #                 server_tags[sc] = tag
    #                 print(f"Found server class {sc} in tag #{tag}")
    #
    #     print(f"Found {len(server_tags)} server class references")


if __name__ == "__main__":
    main()
