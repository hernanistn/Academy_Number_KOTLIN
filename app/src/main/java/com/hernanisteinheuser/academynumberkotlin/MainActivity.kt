package com.hernanisteinheuser.academynumberkotlin


import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.widget.Button
import android.widget.TextView
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.hernanisteinheuser.academynumberkotlin.model.Pessoa
import com.opencsv.CSVWriter
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest
import java.io.*
import java.lang.Exception
import java.text.Normalizer
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks { //lib de permissões de acesso
    val PICK_CSV_FILE = 2
    val REQUEST_ACESS = 182
    val REQUEST_WRITE = 183
    var dataClasses: ArrayList<Pessoa>? = null
    private lateinit var textView: TextView
    private lateinit var btn_choose_file: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
        when {
            intent?.action == Intent.ACTION_SEND -> { //se caso receber o arquivo csv compartilhado
                if ("text/comma-separated-values" == intent.type) {
                    try{
                        val bundle = intent.extras
                        val uri = bundle!!.get(Intent.EXTRA_STREAM) as Uri
                        csvToListPessoa(uri)
                        setTextWithCSV()
                        salvarCVSOrdemAlfabética()
                    }catch (e:Exception){
                        textView.text = applicationContext.getString(R.string.error_text)
                    }
                }
            }
        }
        btn_choose_file.setOnClickListener {
            askPermissionExternalStorage()
            askPermissionWriteExternalStorage()
            chooseFile()
        }
    }

    ////functions
    @ExperimentalStdlibApi
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PICK_CSV_FILE && resultCode == Activity.RESULT_OK) {
            try {
                csvToListPessoa(data!!.data!!)
                setTextWithCSV()
                salvarCVSOrdemAlfabética()
            } catch (e: Exception) {
                textView.text = applicationContext.getString(R.string.error_text)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun initView() {
        textView = findViewById(R.id.text_view)
        btn_choose_file = findViewById(R.id.choose_file)
    }

    private fun chooseFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            type = "text/comma-separated-values"
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, PICK_CSV_FILE)
        }
        startActivityForResult(intent, PICK_CSV_FILE)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun askPermissionExternalStorage() {
        EasyPermissions.requestPermissions(
            PermissionRequest.Builder(
                this,
                REQUEST_ACESS,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
                .setRationale("A permissão é necessária para que o aplicativo funcione.")
                .setPositiveButtonText("Ok")
                .setNegativeButtonText("Cancelar")
                .build()
        )
    }

    private fun askPermissionWriteExternalStorage() {
        EasyPermissions.requestPermissions(
            PermissionRequest.Builder(
                this,
                REQUEST_WRITE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
                .setRationale("A permissão é necessária para que o aplicativo funcione.")
                .setPositiveButtonText("Ok")
                .setNegativeButtonText("Cancelar")
                .build()
        )
    }

    private fun csvToListPessoa(data: Uri) {
        val asas: InputStream
        asas = contentResolver.openInputStream(data)!!
        dataClasses = arrayListOf()
        val data = csvReader().readAllWithHeader(asas)
        data.forEach {
            it.forEach {
                val valuesplited = it.value.split(';')
                dataClasses!!.add(
                    Pessoa(
                        valuesplited.get(0),
                        valuesplited.get(1),
                        valuesplited.get(2).filter { it.isDigit() }.toInt(),
                        valuesplited.get(3)
                    )
                )
            }
        }
    }

    private fun getPorcentagemDeCandidatosPorVaga(vaga: String): Int {
        var count = 0
        dataClasses!!.forEach {
            if (it.vaga == vaga) {
                count++
            }
        }
        return (100 * count) / dataClasses!!.size
    }

    private fun getIdadeMediaCandidatosPorVaga(vaga: String): Int {
        var sumAge = 0
        var count = 0
        dataClasses!!.forEach {
            if (it.vaga == vaga) {
                sumAge += it.idade!!
                count++
            }
        }
        return sumAge / count
    }

    private fun getQuantidadeEstadosPresentes(): Int {
        val estado = arrayListOf<String>()
        dataClasses!!.forEach {
            if (!estado.contains(it.estado)) {
                estado.add(it.estado)
            }
        }
        return estado.size
    }

    private fun getQuantidadeDeCandidatosPorEstado(): HashMap<String, Int> {
        val map: HashMap<String, Int> = hashMapOf()
        dataClasses!!.forEach {
            if (map.containsKey(it.estado)) {
                map.set(it.estado, map.get(it.estado)!! + 1)
            } else {
                map.put(it.estado, 1)
            }
        }
        return map.toList().sortedBy { (_, value) -> value }.toMap() as HashMap
    }

    private fun getProfessorAndroid(): Pessoa {
        var professor: ArrayList<Pessoa> = arrayListOf()
        var auxiliar: ArrayList<Pessoa> = arrayListOf()
        dataClasses!!.forEach { //filtra somente se o primeiro nome possui vogais
            val primeiroNome = Normalizer.normalize(it.nome.toLowerCase(), Normalizer.Form.NFD)
                .replace("[^\\p{ASCII}]", "")
                .split(" ") //separa o primeiro nome dos demais, com o index 0
            for (i in 0..primeiroNome.size - 1) {
                val letraAtual = primeiroNome.first()[i]
                when (letraAtual) {
                    'a', 'e', 'i', 'o', 'u' -> professor.add(it) //adiciona a lista de professor aquele que possui vogal em seu primeiro nome
                }
            }
        }

        professor.forEach { //verifica se a pessoa possui 3 vogais em seu primeiro nome
            val primeiroNome = Normalizer.normalize(it.nome.toLowerCase(), Normalizer.Form.NFD)
                .replace("[^\\p{ASCII}]", "").split(" ")
                .first() //NORMALIZER PARA NÃO IGNORAR ACENTOS
            var count = 0
            for (i in 0..primeiroNome.length - 1) {
                val letraAtual = primeiroNome[i]
                when (letraAtual) {
                    'a', 'e', 'i', 'o', 'u' -> count++
                }
            }
            if (count == 3) {
                auxiliar.add(it)
            }
        }
        professor = auxiliar
        auxiliar = arrayListOf()

        professor.forEach { //verifica se a última letra da pessoa é a vogal 'O'
            val primeiroNome = it.nome.toLowerCase().split(" ")
            if (primeiroNome.first().last() == 'o') {
                auxiliar.add(it)
            }
        }
        professor = auxiliar
        auxiliar = arrayListOf()

        professor.forEach { //verifica se a idade é ímpar
            if (it.idade!! % 2 != 0) {
                auxiliar.add(it)
            }
        }
        professor = auxiliar
        auxiliar = arrayListOf()

        professor.forEach { //Verifica se a pessoa tem menos de 31 anos e se pertence a mesma década que o professor IOS
            if (it.idade!! < 31 && pertenceAMesmaDecada(it.idade!!, getProfessorIOS().idade!!)) {
                auxiliar.add(it)
            }
        }
        professor = auxiliar
        auxiliar = arrayListOf()

        professor.forEach { //verifica se o estado da Pessoa é SC
            if (it.estado == "SC") {
                auxiliar.add(it)
            }
        }
        professor = auxiliar
        auxiliar = arrayListOf()

        professor.forEach { //filtra se a vaga da pessoa não pertence a Android -> "a vaga atribuída aos instrutores (na planilha) não é a vaga na qual eles vão instruir"
            if (it.vaga != "Android") {
                auxiliar.add(it)
            }
        }
        professor = auxiliar
        auxiliar = arrayListOf()

        return professor.first()
    }

    private fun getProfessorIOS(): Pessoa {
        var professor = arrayListOf<Pessoa>()
        var auxiliar = arrayListOf<Pessoa>()
        dataClasses!!.forEach {//idade maior que 20, se numero primo e verifica se a vaga não pertence ao iOS
            if (it.idade!! > 20 && isNumeroPrimo(it.idade!!) && it.vaga != "iOS") {
                professor.add(it)
            }
        }

        professor.forEach { //veririfica se o segundo nome inicia com a letra 'v'
            val nomeNome = it.nome.split(" ").get(1).toCharArray()
            if (nomeNome[0].toLowerCase() == 'v') {
                auxiliar.add(it)
            }
        }
        professor = auxiliar
        auxiliar = arrayListOf()

        return professor.first()
    }

    private fun isNumeroPrimo(idade: Int): Boolean {
        return !((idade % 2) == 0 && idade != 2 || idade == 1)
    }

    private fun pertenceAMesmaDecada(idade1: Int, idade2: Int): Boolean {
        val anoIdade1 =
            (Calendar.getInstance().get(Calendar.YEAR) - idade1).toString().toCharArray()[2] //retorna a decada (ex: 1980 = '8')
        val anoIdade2 =
            (Calendar.getInstance().get(Calendar.YEAR) - idade2).toString().toCharArray()[2]
        return anoIdade1 == anoIdade2
    }

    private fun setTextWithCSV() {
        textView.text = "Proporção de candidatos por vaga:\n" +
                "Android:  ${getPorcentagemDeCandidatosPorVaga("Android")}% \n" +
                "iOS:  ${getPorcentagemDeCandidatosPorVaga("iOS")}% \n" +
                "QA:  ${getPorcentagemDeCandidatosPorVaga("QA")}% \n \n" +
                "Idade média dos canditados de QA: ${getIdadeMediaCandidatosPorVaga("QA")}\n\n" +
                "Número de estados distintos presente na lista: ${getQuantidadeEstadosPresentes()}\n\n" +
                "Rank dos 2 estados com menos ocorrências:\n" +
                "#1 ${
                    getQuantidadeDeCandidatosPorEstado().toList().get(0).first
                } - ${getQuantidadeDeCandidatosPorEstado().toList().get(0).second} candidatos\n" +
                "#2 ${
                    getQuantidadeDeCandidatosPorEstado().toList().get(1).first
                } - ${getQuantidadeDeCandidatosPorEstado().toList().get(1).second} candidatos\n\n" +
                "Instrutor Android: ${getProfessorAndroid().nome}\n" +
                "Instrutor iOS: ${getProfessorIOS().nome}"
    }

    private fun salvarCVSOrdemAlfabética() {
        val csv =
            Environment.getExternalStorageDirectory().absolutePath + "/Sorted_AppAcademy_Candidates.csv"
        textView.text = textView.text.toString()+"\n\nLista sorteada salva no caminho: "+csv
        val header = arrayOf("Nome", "Vaga", "Idade", "Estado") //cabeçalho do CSV
        val csvWriter = CSVWriter(FileWriter(csv))
        csvWriter.writeNext(header)
        dataClasses!!.sortBy { it.nome }
        dataClasses!!.forEach {
            csvWriter.writeNext(arrayOf(it.nome, it.vaga, it.idade.toString(), it.estado))
        }
        csvWriter.flush()
        csvWriter.close()
    }
}


/* Notas de dificuldade:
Dificuldade em interpretar FileWrite, FileOutPutStream e FileInputStream (java.io em geral).
Utilizei uma lib que me ajudou a pular o processo de leitura - kotlin-csv
Apesar da kotlin-csv poder fazer a função de WRITE, sempre havia erros que apesar de observar
vários exemplos encontrados na web, não houve solução. Então encontrei outra lib que facilitou
o processo de WRITE - opencsv, por isso que se caso observar as dependencias do projetos irá
se deparar com 2 libs de manipulam arquivos .csv



 */