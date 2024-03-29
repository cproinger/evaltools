import groovy.xml.StreamingMarkupBuilder

def startTime = System.currentTimeMillis()

// Put path to SOLIS XHive files here
def base = ".."
def inputPath = new File(base, "Solis-solr-temp")
def outputPath = new File(base, "Solis-solr")
outputPath.mkdir()
def girtIDs = []
new File(base, 'resources/GIRT-IDs.txt').eachLine {girtIDs << it.toString().trim()}

// Generate id:topics map
def idTopicMap = [:]
def qrelFiles = new File(base, "qrels").list().toList()

qrelFiles.each {qrelFile ->

    def qrels = new File(base, "qrels/${qrelFile}")
// fill map
    // TODO: totaler Bullshit das mit einer CSV Zeile zu machen, aber eine Liste in
    // einer Map wollte einfach ums Verrecken nicht klappen... :-/
    qrels.eachLine {line ->
        def topic = line.tokenize()[0]
        def id = line.tokenize()[2]
        idTopicMap[id.toString()] += ";${topic}"
        idTopicMap[id.toString()] = idTopicMap[id.toString()].toString().replace("null;", "")
    }
}

println "Found ${girtIDs.size()} GIRT4 IDs"
println "Found ${idTopicMap.size()} GIRT4 IDs Topic Mappings"

def xHiveFiles = inputPath.list().toList()

// Die se Funktion implementiert den girt:true Eintrag nur, wenn die id auch irgendwann einmal bewertet wurde.
// mit der Änderung in der inneren Schleife werden alle girtIDs verwendet!

xHiveFiles.each {xHiveFile ->
    println "Including GIRT Flag for ${xHiveFile}"

    def xml = new XmlParser().parse(new File(inputPath, xHiveFile))

    xml.doc.each {doc ->
        def id = doc.field.find {it.@name == 'acquisition_id'}.text().toString().trim()

        //TODO: Auch hier ist das Gemunkel mit der CSV totaler Quatsch... s.o.
        //if (idTopicMap.keySet().contains("GIRT-DE" + id)) {
        if (girtIDs.contains("GIRT-DE" + id)) {
            doc.appendNode('field', [name: 'girt'], "true")
            idTopicMap["GIRT-DE" + id]?.tokenize(";").each {topicNum ->
                doc.appendNode('field', [name: 'girt_topic'], topicNum)
            }
        }
        else {
            doc.appendNode('field', [name: 'girt'], "false")
        }

    }

    // write the updated XML file
    def writer = new PrintWriter(new File(outputPath, xHiveFile))
    new XmlNodePrinter(writer).print(xml)


}

println "Converting duration in total: ${(System.currentTimeMillis() - startTime) / 1000} secs"
