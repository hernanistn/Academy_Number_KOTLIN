package com.hernanisteinheuser.academynumberkotlin


import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.hernanisteinheuser.academynumberkotlin.model.Pessoa
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest
import java.io.*
import java.lang.Exception
import java.text.Normalizer
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks /*lib de permissões de acesso*/{
    val PICK_CSV_FILE = 2
    val REQUEST_ACESS = 182
    val REQUEST_WRITE = 183
    var dataClasses: ArrayList<Pessoa>? = null
    private lateinit var textView: TextView
    private lateinit var btn_choose_file: Button
    @RequiresApi(Build.VERSION_CODES.N)
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
                        csvToPessoa(uri)
                        setTextWithInfos()
                        salvarCSV()
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
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) { //trata o arquivo que o usuário seleciona
        if (requestCode == PICK_CSV_FILE && resultCode == Activity.RESULT_OK) {
            try {
                csvToPessoa(data!!.data!!)
                setTextWithInfos()
                salvarCSV()
            } catch (e: Exception) {
                textView.text = applicationContext.getString(R.string.error_text)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun initView() {
        textView = findViewById(R.id.text_view)
        btn_choose_file = findViewById(R.id.choose_file)
    } //inicia a View da Activity - void

    private fun chooseFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            type = "text/comma-separated-values"
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, PICK_CSV_FILE)
        }
        startActivityForResult(intent, PICK_CSV_FILE)
    }//Intent que inicia a tela de selecionar arquivos (text/comma-separated-values) - void

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
    } //verifica se a permissão foi concebida - void

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
    } //exige permissão de acesso para acessar a memória - void

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
    } //exige permissão de acesso para escrever na memória - void

    @RequiresApi(Build.VERSION_CODES.N)
    private fun csvToPessoa(data: Uri){
        dataClasses = arrayListOf()
        val br = BufferedReader(contentResolver.openInputStream(data)!!.bufferedReader())
        while (br.ready()){
            br.lines().forEach {
                if(!it.contains("Nome;Vaga;Idade;Estado")){ //remove o cabeçalho
                   val splited = it.split(';')
                    dataClasses!!.add(
                        Pessoa(
                            splited.get(0),
                            splited.get(1),
                            splited.get(2).filter { it.isDigit() }.toInt(),
                            splited.get(3)
                        )
                    )
                }
            }
        }
    } //transforma CSV em Objeto Pessoa - void

    private fun getPorcentagemDeCandidatosPorVaga(vaga: String): Int {
        var count = 0
        dataClasses!!.forEach {
            if (it.vaga == vaga) {
                count++
            }
        }
        return (100 * count) / dataClasses!!.size
    } //retorna a porcentagem de candidatos por vaga - return Int

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
    } //retorna a média de idade por vaga - return Int

    private fun getQuantidadeEstadosPresentes(): Int {
        val estado = arrayListOf<String>()
        dataClasses!!.forEach {
            if (!estado.contains(it.estado)) {
                estado.add(it.estado)
            }
        }
        return estado.size
    } // retorna quantidade de estados que o CSV guarda - return Int

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
    } //retorna os candidatos e seu respectivo estado - return HashMap

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
    } //retorna o professor Android - return Pessoa

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
    } //retorna o professor IOS - return Pessoa

    private fun isNumeroPrimo(idade: Int): Boolean {
        return !((idade % 2) == 0 && idade != 2 || idade == 1)
    } //verifica se o número/idade é um número primo - return Boolean

    private fun pertenceAMesmaDecada(idade1: Int, idade2: Int): Boolean {
        val anoIdade1 =
            (Calendar.getInstance().get(Calendar.YEAR) - idade1).toString().toCharArray()[2] //retorna a decada (ex: 1980 = '8')
        val anoIdade2 =
            (Calendar.getInstance().get(Calendar.YEAR) - idade2).toString().toCharArray()[2]
        return anoIdade1 == anoIdade2
    } //verifica se as idades pertencem a mesma década - return Boolean

    private fun setTextWithInfos() {
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
    } //Altera o texto principal com as informações exigidas - void

    private fun salvarCSV(){
        val caminho = File(externalCacheDir!!.absolutePath+"/Sorted_AppAcademy_Candidates.csv")
        val fw = FileWriter(caminho)
        val bf = BufferedWriter(fw)
        textView.text = textView.text.toString()+"\n\n"+"Lista em ordem alfabetica salva em: "+caminho.path
        bf.write("Nome;Vaga;Idade;Estado")
        bf.newLine()
        dataClasses!!.sortBy { it.nome }
        dataClasses!!.forEach {
            bf.write("${it.nome};${it.vaga};${it.idade};${it.estado}")
            bf.newLine()
        }
        bf.flush()
        bf.close()
    } //salva o csv em ordem alfabética na paste de cache do app - void
}
